package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.ai.repository.AiMessageRepository;
import org.rap.algotutorbe.ai.repository.ConversationRepository;
import org.rap.algotutorbe.ai.tools.AlgoTutorAiTools;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.repositories.LessonRepository;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatServiceValidationTest {

    private AiChatService aiChatService;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AiContextService contextService = mock(AiContextService.class);
        AiPromptService promptService = mock(AiPromptService.class);
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ProviderRouter providerRouter = mock(ProviderRouter.class);
        LessonRepository lessonRepository = mock(LessonRepository.class);
        AiMessageRepository aiMessageRepository = mock(AiMessageRepository.class);
        AlgoTutorAiTools algoTutorAiTools = mock(AlgoTutorAiTools.class);
        aiChatService = new AiChatService(contextService, promptService, conversationRepository, providerRouter, lessonRepository, aiMessageRepository, algoTutorAiTools);

        // Mock conversation save to return entity with ID (so chat() can proceed past validation)
        when(conversationRepository.save(any(AIConversation.class)))
                .thenAnswer(invocation -> {
                    AIConversation conv = invocation.getArgument(0);
                    if (conv.getId() == null) {
                        conv.setId(UUID.randomUUID());
                    }
                    return conv;
                });

        // Mock empty history
        when(aiMessageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(any(UUID.class)))
                .thenReturn(Collections.emptyList());
    }

    // --- Mode validation tests ---

    @Test
    void chat_invalidMode_throwsInvalidChatMode() {
        AiChatRequest request = buildRequest("INVALID_MODE", "hello", null);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_CHAT_MODE);
    }

    @Test
    void chat_nullMode_throwsInvalidChatMode() {
        AiChatRequest request = buildRequest(null, "hello", null);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_CHAT_MODE);
    }

    @Test
    void chat_emptyMode_throwsInvalidChatMode() {
        AiChatRequest request = buildRequest("", "hello", null);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_CHAT_MODE);
    }

    @ParameterizedTest
    @EnumSource(AiChatMode.class)
    void chat_allValidModes_doesNotThrowInvalidChatMode(AiChatMode mode) {
        AiChatRequest request = buildRequest(mode.name(), "hello", "some code");

        // Should not throw INVALID_CHAT_MODE (may return null since chat() is not fully implemented)
        try {
            aiChatService.chat(request, TEST_USER_ID);
        } catch (AppException e) {
            assertThat(e.getError()).isNotEqualTo(ErrorCode.INVALID_CHAT_MODE);
        }
    }

    @Test
    void chat_caseInsensitiveMode_accepted() {
        AiChatRequest request = buildRequest("hint", "hello", null);

        // Should not throw INVALID_CHAT_MODE
        try {
            aiChatService.chat(request, TEST_USER_ID);
        } catch (AppException e) {
            assertThat(e.getError()).isNotEqualTo(ErrorCode.INVALID_CHAT_MODE);
        }
    }

    // --- Code-required modes tests ---

    @ParameterizedTest
    @ValueSource(strings = {"DEBUG", "REVIEW", "COMPLEXITY"})
    void chat_codeRequiredMode_withNullCode_throwsCodeRequired(String mode) {
        AiChatRequest request = buildRequest(mode, "help me debug", null);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.CODE_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBUG", "REVIEW", "COMPLEXITY"})
    void chat_codeRequiredMode_withBlankCode_throwsCodeRequired(String mode) {
        AiChatRequest request = buildRequest(mode, "help me debug", "   ");

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.CODE_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEBUG", "REVIEW", "COMPLEXITY"})
    void chat_codeRequiredMode_withValidCode_doesNotThrowCodeRequired(String mode) {
        AiChatRequest request = buildRequest(mode, "help me", "int x = 1;");

        try {
            aiChatService.chat(request, TEST_USER_ID);
        } catch (AppException e) {
            assertThat(e.getError()).isNotEqualTo(ErrorCode.CODE_REQUIRED);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"HINT", "EXPLAIN", "SOLUTION", "NEXT_STEP"})
    void chat_nonCodeRequiredMode_withNullCode_doesNotThrowCodeRequired(String mode) {
        AiChatRequest request = buildRequest(mode, "hello", null);

        try {
            aiChatService.chat(request, TEST_USER_ID);
        } catch (AppException e) {
            assertThat(e.getError()).isNotEqualTo(ErrorCode.CODE_REQUIRED);
        }
    }

    // --- At least one of message/code non-blank tests ---

    @Test
    void chat_bothMessageAndCodeNull_throwsInvalidPayload() {
        AiChatRequest request = buildRequest("HINT", null, null);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }

    @Test
    void chat_bothMessageAndCodeBlank_throwsInvalidPayload() {
        AiChatRequest request = buildRequest("HINT", "   ", "   ");

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }

    @Test
    void chat_messageNullCodeBlank_throwsInvalidPayload() {
        AiChatRequest request = buildRequest("HINT", null, "  ");

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }

    @Test
    void chat_messageBlankCodeNull_throwsInvalidPayload() {
        AiChatRequest request = buildRequest("HINT", "  ", null);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }

    // --- Length limit tests ---

    @Test
    void chat_messageExceeds5000Chars_throwsInvalidPayload() {
        String longMessage = "a".repeat(5001);
        AiChatRequest request = buildRequest("HINT", longMessage, null);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }

    @Test
    void chat_messageExactly5000Chars_doesNotThrowForLength() {
        String message = "a".repeat(5000);
        AiChatRequest request = buildRequest("HINT", message, null);

        try {
            aiChatService.chat(request, TEST_USER_ID);
        } catch (AppException e) {
            // Should not be INVALID_PAYLOAD for length reasons
            // (it's valid length, so any exception would be from other logic)
            assertThat(e.getError()).isNotEqualTo(ErrorCode.INVALID_PAYLOAD);
        }
    }

    @Test
    void chat_codeExceeds10000Chars_throwsInvalidPayload() {
        String longCode = "x".repeat(10001);
        AiChatRequest request = buildRequest("DEBUG", "help", longCode);

        assertThatThrownBy(() -> aiChatService.chat(request, TEST_USER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getError())
                .isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }

    @Test
    void chat_codeExactly10000Chars_doesNotThrowForLength() {
        String code = "x".repeat(10000);
        AiChatRequest request = buildRequest("DEBUG", "help", code);

        try {
            aiChatService.chat(request, TEST_USER_ID);
        } catch (AppException e) {
            assertThat(e.getError()).isNotEqualTo(ErrorCode.INVALID_PAYLOAD);
        }
    }

    // --- Helper ---

    private AiChatRequest buildRequest(String mode, String message, String code) {
        return new AiChatRequest(
                null, null, null, null, mode,
                message, code, null,
                null, null, null
        );
    }
}
