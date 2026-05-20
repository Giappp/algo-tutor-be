package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Answers.RETURNS_SELF;

class AiChatServiceConversationTest {

    private AiChatService aiChatService;
    private ConversationRepository conversationRepository;
    private LessonRepository lessonRepository;
    private ProviderRouter providerRouter;

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AiContextService contextService = mock(AiContextService.class);
        AiPromptService promptService = mock(AiPromptService.class);
        conversationRepository = mock(ConversationRepository.class);
        providerRouter = mock(ProviderRouter.class);
        lessonRepository = mock(LessonRepository.class);
        AiMessageRepository aiMessageRepository = mock(AiMessageRepository.class);
        AlgoTutorAiTools algoTutorAiTools = mock(AlgoTutorAiTools.class);
        aiChatService = new AiChatService(contextService, promptService, conversationRepository, providerRouter, lessonRepository, aiMessageRepository, algoTutorAiTools);

        // Default: save returns the entity passed in
        when(conversationRepository.save(any(AIConversation.class)))
                .thenAnswer(invocation -> {
                    AIConversation conv = invocation.getArgument(0);
                    if (conv.getId() == null) {
                        conv.setId(UUID.randomUUID());
                    }
                    return conv;
                });

        // Default: return empty history
        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(any(UUID.class)))
                .thenReturn(Collections.emptyList());

        // Default: mock ChatClient chain for LLM call
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(providerRouter.route(any())).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("AI response");
    }

    // --- Create new conversation tests ---

    @Test
    void findOrCreateConversation_nullConversationId_createsNewConversation() {
        AiChatRequest request = buildRequest(null, "Hello AI", null, null, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getProvider()).isEqualTo(LLMProvider.OPENAI);
        verify(conversationRepository).save(any(AIConversation.class));
    }

    @Test
    void findOrCreateConversation_nullConversationId_setsLessonId() {
        Long lessonId = 42L;
        AiChatRequest request = buildRequest(null, "Hello AI", null, lessonId, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.GEMINI);

        assertThat(result.getLessonId()).isEqualTo(lessonId);
    }

    @Test
    void findOrCreateConversation_nullConversationId_setsTitleFromMessage() {
        AiChatRequest request = buildRequest(null, "How do I implement binary search?", null, null, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI);

        assertThat(result.getTitle()).isEqualTo("How do I implement binary search?");
    }

    @Test
    void findOrCreateConversation_nullConversationId_truncatesTitleTo255Chars() {
        String longMessage = "a".repeat(300);
        AiChatRequest request = buildRequest(null, longMessage, null, null, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI);

        assertThat(result.getTitle()).hasSize(255);
    }

    @Test
    void findOrCreateConversation_nullMessageWithLessonId_setsTitleFromLesson() {
        Long lessonId = 10L;
        Lesson lesson = mock(Lesson.class);
        when(lesson.getTitle()).thenReturn("Binary Search Algorithm");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        AiChatRequest request = buildRequest(null, null, "int x = 1;", lessonId, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI);

        assertThat(result.getTitle()).isEqualTo("Binary Search Algorithm");
    }

    @Test
    void findOrCreateConversation_nullMessageNoLesson_setsDefaultTitle() {
        AiChatRequest request = buildRequest(null, null, "int x = 1;", null, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI);

        assertThat(result.getTitle()).isEqualTo("New Conversation");
    }

    @Test
    void findOrCreateConversation_blankMessage_fallsBackToLessonTitle() {
        Long lessonId = 5L;
        Lesson lesson = mock(Lesson.class);
        when(lesson.getTitle()).thenReturn("Sorting Algorithms");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        AiChatRequest request = buildRequest(null, "   ", "code here", lessonId, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI);

        assertThat(result.getTitle()).isEqualTo("Sorting Algorithms");
    }

    // --- Find existing conversation tests ---

    @Test
    void findOrCreateConversation_existingConversationId_returnsConversation() {
        UUID conversationId = UUID.randomUUID();
        AIConversation existing = new AIConversation();
        existing.setId(conversationId);
        existing.setUserId(TEST_USER_ID);
        existing.setProvider(LLMProvider.OPENAI);

        when(conversationRepository.findByIdAndUserId(conversationId, TEST_USER_ID))
                .thenReturn(Optional.of(existing));

        AiChatRequest request = buildRequest(conversationId, "Follow up question", null, null, null);

        AIConversation result = aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI);

        assertThat(result).isEqualTo(existing);
        assertThat(result.getId()).isEqualTo(conversationId);
    }

    @Test
    void findOrCreateConversation_conversationNotFound_throwsConversationNotFound() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(conversationId, TEST_USER_ID))
                .thenReturn(Optional.empty());

        AiChatRequest request = buildRequest(conversationId, "Hello", null, null, null);

        assertThatThrownBy(() -> aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    void findOrCreateConversation_wrongUserId_throwsConversationNotFound() {
        UUID conversationId = UUID.randomUUID();
        // findByIdAndUserId returns empty when userId doesn't match
        when(conversationRepository.findByIdAndUserId(conversationId, TEST_USER_ID))
                .thenReturn(Optional.empty());

        AiChatRequest request = buildRequest(conversationId, "Hello", null, null, null);

        assertThatThrownBy(() -> aiChatService.findOrCreateConversation(request, TEST_USER_ID, LLMProvider.OPENAI))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    // --- Provider resolution tests ---

    @Test
    void chat_nullProvider_usesDefaultProvider() {
        AiChatRequest request = buildRequest(null, "Hello", null, null, null);

        aiChatService.chat(request, TEST_USER_ID);

        // save is called twice: once for creation, once for updatedAt update
        verify(conversationRepository, atLeastOnce()).save(argThat(conv ->
                conv.getProvider() == LLMProvider.OPENAI
        ));
    }

    @Test
    void chat_validProvider_usesSpecifiedProvider() {
        AiChatRequest request = buildRequest(null, "Hello", null, null, "GEMINI");

        aiChatService.chat(request, TEST_USER_ID);

        // save is called twice: once for creation, once for updatedAt update
        verify(conversationRepository, atLeastOnce()).save(argThat(conv ->
                conv.getProvider() == LLMProvider.GEMINI
        ));
    }

    @Test
    void chat_unsupportedProvider_throwsUnsupportedProvider() {
        AiChatRequest request = buildRequest(null, "Hello", null, null, "INVALID_PROVIDER");

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.UNSUPPORTED_PROVIDER);
    }

    // --- updatedAt timestamp test ---

    @Test
    void chat_existingConversation_savesConversationToUpdateTimestamp() {
        UUID conversationId = UUID.randomUUID();
        AIConversation existing = new AIConversation();
        existing.setId(conversationId);
        existing.setUserId(TEST_USER_ID);
        existing.setProvider(LLMProvider.OPENAI);

        when(conversationRepository.findByIdAndUserId(conversationId, TEST_USER_ID))
                .thenReturn(Optional.of(existing));

        AiChatRequest request = buildRequest(conversationId, "Follow up", null, null, null);

        aiChatService.chat(request, TEST_USER_ID);

        // save is called to trigger @PreUpdate for updatedAt
        verify(conversationRepository).save(existing);
    }

    // --- Helper ---

    private AiChatRequest buildRequest(UUID conversationId, String message, String code, Long lessonId, String provider) {
        return new AiChatRequest(
                conversationId, lessonId, null, provider, "HINT",
                message, code, null,
                null, null, null
        );
    }
}
