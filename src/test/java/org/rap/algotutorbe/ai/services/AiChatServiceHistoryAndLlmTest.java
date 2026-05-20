package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.entity.AiMessage;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.AiMessageRole;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiChatServiceHistoryAndLlmTest {

    private AiChatService aiChatService;
    private AiMessageRepository aiMessageRepository;
    private ProviderRouter providerRouter;
    private AlgoTutorAiTools algoTutorAiTools;

    private static final UUID TEST_CONVERSATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AiContextService contextService = mock(AiContextService.class);
        AiPromptService promptService = mock(AiPromptService.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        providerRouter = mock(ProviderRouter.class);
        LessonRepository lessonRepository = mock(LessonRepository.class);
        aiMessageRepository = mock(AiMessageRepository.class);
        algoTutorAiTools = mock(AlgoTutorAiTools.class);
        aiChatService = new AiChatService(contextService, promptService, conversationRepository, providerRouter, lessonRepository, aiMessageRepository, algoTutorAiTools);
    }

    // --- buildConversationHistory tests ---

    @Test
    void buildConversationHistory_noMessages_returnsEmptyString() {
        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID))
                .thenReturn(Collections.emptyList());

        String history = aiChatService.buildConversationHistory(TEST_CONVERSATION_ID);

        assertThat(history).isEmpty();
    }

    @Test
    void buildConversationHistory_singleMessage_returnsFormattedMessage() {
        AiMessage msg = createMessage(AiMessageRole.USER, "Hello AI", Instant.now());
        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID))
                .thenReturn(List.of(msg));

        String history = aiChatService.buildConversationHistory(TEST_CONVERSATION_ID);

        assertThat(history).isEqualTo("USER: Hello AI");
    }

    @Test
    void buildConversationHistory_multipleMessages_reversedToOldestFirst() {
        // Repository returns DESC order (newest first)
        Instant now = Instant.now();
        AiMessage msg3 = createMessage(AiMessageRole.ASSISTANT, "Here is a hint", now);
        AiMessage msg2 = createMessage(AiMessageRole.USER, "Give me a hint", now.minusSeconds(10));
        AiMessage msg1 = createMessage(AiMessageRole.ASSISTANT, "Hello!", now.minusSeconds(20));

        // DESC order: msg3, msg2, msg1
        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID))
                .thenReturn(new ArrayList<>(List.of(msg3, msg2, msg1)));

        String history = aiChatService.buildConversationHistory(TEST_CONVERSATION_ID);

        // Should be reversed to oldest-first: msg1, msg2, msg3
        assertThat(history).isEqualTo(
                "ASSISTANT: Hello!\n" +
                "USER: Give me a hint\n" +
                "ASSISTANT: Here is a hint"
        );
    }

    @Test
    void buildConversationHistory_retrievesMax10Messages() {
        // Verify the repository method is called (which already limits to 10)
        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID))
                .thenReturn(Collections.emptyList());

        aiChatService.buildConversationHistory(TEST_CONVERSATION_ID);

        verify(aiMessageRepository).findTop10ByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID);
    }

    @Test
    void buildConversationHistory_exceedsContextWindow_truncatesToMin4Messages() {
        // Create messages where the full history exceeds MAX_CONTEXT_WINDOW_CHARS (100,000)
        Instant now = Instant.now();
        String longContent = "x".repeat(30_000); // Each message ~30K chars

        // 5 messages in DESC order (newest first)
        List<AiMessage> messages = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            messages.add(createMessage(
                    i % 2 == 0 ? AiMessageRole.USER : AiMessageRole.ASSISTANT,
                    longContent + i,
                    now.minusSeconds(i * 10L)
            ));
        }

        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID))
                .thenReturn(messages);

        String history = aiChatService.buildConversationHistory(TEST_CONVERSATION_ID);

        // Should be truncated to 4 most recent messages
        // The history should contain exactly 4 messages (3 newline separators)
        long lineCount = history.chars().filter(c -> c == '\n').count();
        assertThat(lineCount).isEqualTo(3); // 4 messages = 3 newlines between them
    }

    @Test
    void buildConversationHistory_exceedsContextWindow_but4OrFewerMessages_noTruncation() {
        // 4 messages with very long content — should NOT truncate since we're at the minimum
        Instant now = Instant.now();
        String longContent = "x".repeat(30_000);

        List<AiMessage> messages = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            messages.add(createMessage(
                    i % 2 == 0 ? AiMessageRole.USER : AiMessageRole.ASSISTANT,
                    longContent + i,
                    now.minusSeconds(i * 10L)
            ));
        }

        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(TEST_CONVERSATION_ID))
                .thenReturn(messages);

        String history = aiChatService.buildConversationHistory(TEST_CONVERSATION_ID);

        // All 4 messages should be present (3 newlines)
        long lineCount = history.chars().filter(c -> c == '\n').count();
        assertThat(lineCount).isEqualTo(3);
    }

    // --- callLlm tests ---

    @Test
    void callLlm_successfulResponse_returnsContent() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(providerRouter.route("OPENAI")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Here is your hint.");

        String result = aiChatService.callLlm("OPENAI", "system prompt", "user prompt");

        assertThat(result).isEqualTo("Here is your hint.");
    }

    @Test
    void callLlm_nullResponse_throwsAiServiceUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(providerRouter.route("OPENAI")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(null);

        assertThatThrownBy(() -> aiChatService.callLlm("OPENAI", "system", "user"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void callLlm_blankResponse_throwsAiServiceUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(providerRouter.route("OPENAI")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("   ");

        assertThatThrownBy(() -> aiChatService.callLlm("OPENAI", "system", "user"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void callLlm_timeoutException_throwsAiServiceUnavailable() {
        when(providerRouter.route("OPENAI")).thenThrow(new RuntimeException("Connection timed out"));

        assertThatThrownBy(() -> aiChatService.callLlm("OPENAI", "system", "user"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void callLlm_providerError_throwsAiServiceUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);

        when(providerRouter.route("OPENAI")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Provider rate limited"));

        assertThatThrownBy(() -> aiChatService.callLlm("OPENAI", "system", "user"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void callLlm_unsupportedProvider_rethrowsAppException() {
        when(providerRouter.route("INVALID")).thenThrow(new AppException(ErrorCode.UNSUPPORTED_PROVIDER));

        assertThatThrownBy(() -> aiChatService.callLlm("INVALID", "system", "user"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.UNSUPPORTED_PROVIDER);
    }

    @Test
    void callLlm_socketTimeoutException_throwsAiServiceUnavailable() {
        when(providerRouter.route("OPENAI")).thenThrow(
                new RuntimeException("Request failed", new java.net.SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> aiChatService.callLlm("OPENAI", "system", "user"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void callLlm_invalidApiKeyError_throwsAiServiceUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);

        when(providerRouter.route("OPENAI")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Invalid API key: sk-abc123..."));

        AppException thrown = (AppException) org.junit.jupiter.api.Assertions.assertThrows(
                AppException.class, () -> aiChatService.callLlm("OPENAI", "system", "user"));

        // Verify the error code is AI_SERVICE_UNAVAILABLE (no internal details exposed)
        assertThat(thrown.getError()).isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
        // Verify the exception message is the error code key, not the original error message
        assertThat(thrown.getMessage()).doesNotContain("sk-abc123");
        assertThat(thrown.getMessage()).doesNotContain("Invalid API key");
    }

    @Test
    void callLlm_connectionStringError_doesNotExposeInternalDetails() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);

        when(providerRouter.route("OPENAI")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Connection to https://api.openai.com failed with key sk-secret123"));

        AppException thrown = (AppException) org.junit.jupiter.api.Assertions.assertThrows(
                AppException.class, () -> aiChatService.callLlm("OPENAI", "system", "user"));

        // The AppException message should be the error code key, not the internal error
        assertThat(thrown.getMessage()).isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE.getKey());
        assertThat(thrown.getMessage()).doesNotContain("sk-secret123");
        assertThat(thrown.getMessage()).doesNotContain("api.openai.com");
    }

    @Test
    void callLlm_registersToolsWithChatClient() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(providerRouter.route("OPENAI")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("response");

        aiChatService.callLlm("OPENAI", "system", "user");

        // Verify tools were registered
        verify(requestSpec).tools(algoTutorAiTools);
    }

    // --- Helper ---

    private AiMessage createMessage(AiMessageRole role, String content, Instant createdAt) {
        AiMessage msg = new AiMessage();
        msg.setId(UUID.randomUUID());
        msg.setConversationId(TEST_CONVERSATION_ID);
        msg.setUserId(UUID.randomUUID());
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(createdAt);
        return msg;
    }
}
