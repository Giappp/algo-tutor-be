package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProviderRouterTest {

    private ChatClient openAiClient;
    private ChatClient geminiClient;
    private ChatClient claudeClient;
    private ProviderRouter router;

    @BeforeEach
    void setUp() {
        openAiClient = mock(ChatClient.class);
        geminiClient = mock(ChatClient.class);
        claudeClient = mock(ChatClient.class);
        router = new ProviderRouter(openAiClient, geminiClient, claudeClient, LLMProvider.GEMINI);
    }

    @Test
    void route_withOpenAiProvider_returnsOpenAiClient() {
        ChatClient result = router.route("OPENAI");
        assertThat(result).isSameAs(openAiClient);
    }

    @Test
    void route_withGeminiProvider_returnsGeminiClient() {
        ChatClient result = router.route("GEMINI");
        assertThat(result).isSameAs(geminiClient);
    }

    @Test
    void route_withClaudeProvider_returnsClaudeClient() {
        ChatClient result = router.route("CLAUDE_SONNET_4_6");
        assertThat(result).isSameAs(claudeClient);
    }

    @Test
    void route_withLowercaseProvider_routesCorrectly() {
        ChatClient result = router.route("openai");
        assertThat(result).isSameAs(openAiClient);
    }

    @Test
    void route_withMixedCaseProvider_routesCorrectly() {
        ChatClient result = router.route("Gemini");
        assertThat(result).isSameAs(geminiClient);
    }

    @Test
    void route_withNullProvider_usesDefaultProvider() {
        ChatClient result = router.route(null);
        assertThat(result).isSameAs(geminiClient);
    }

    @Test
    void route_withBlankProvider_usesDefaultProvider() {
        ChatClient result = router.route("   ");
        assertThat(result).isSameAs(geminiClient);
    }

    @Test
    void route_withEmptyProvider_usesDefaultProvider() {
        ChatClient result = router.route("");
        assertThat(result).isSameAs(geminiClient);
    }

    @Test
    void route_withUnsupportedProvider_throwsAppException() {
        assertThatThrownBy(() -> router.route("GPT4"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getError())
                        .isEqualTo(ErrorCode.UNSUPPORTED_PROVIDER));
    }

    @Test
    void route_withInvalidProvider_throwsAppException() {
        assertThatThrownBy(() -> router.route("INVALID_PROVIDER"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getError())
                        .isEqualTo(ErrorCode.UNSUPPORTED_PROVIDER));
    }

    @Test
    void route_withDefaultProviderOpenAi_usesOpenAiWhenNull() {
        ProviderRouter openAiDefaultRouter = new ProviderRouter(
                openAiClient, geminiClient, claudeClient, LLMProvider.OPENAI);
        ChatClient result = openAiDefaultRouter.route(null);
        assertThat(result).isSameAs(openAiClient);
    }

    @Test
    void route_withDefaultProviderClaude_usesClaudeWhenNull() {
        ProviderRouter claudeDefaultRouter = new ProviderRouter(
                openAiClient, geminiClient, claudeClient, LLMProvider.CLAUDE_SONNET_4_6);
        ChatClient result = claudeDefaultRouter.route(null);
        assertThat(result).isSameAs(claudeClient);
    }
}
