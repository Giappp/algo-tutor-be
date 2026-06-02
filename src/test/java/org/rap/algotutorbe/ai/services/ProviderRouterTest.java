package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderRouterTest {

    @Mock
    private ObjectProvider<ChatClient> openAiClientProvider;
    @Mock
    private ObjectProvider<ChatClient> geminiClientProvider;
    @Mock
    private ObjectProvider<ChatClient> claudeClientProvider;

    @Mock
    private ChatClient mockChatClient;

    private ProviderRouter providerRouter;

    @BeforeEach
    void setUp() {
        providerRouter = new ProviderRouter(
                openAiClientProvider,
                geminiClientProvider,
                claudeClientProvider,
                LLMProvider.GEMINI
        );
    }

    @Test
    void route_shouldReturnClientWhenAvailable() {
        when(geminiClientProvider.getIfAvailable()).thenReturn(mockChatClient);

        ChatClient routedClient = providerRouter.route("GEMINI");

        assertThat(routedClient).isSameAs(mockChatClient);
    }

    @Test
    void route_shouldFallbackToDefaultProviderWhenNullOrBlank() {
        when(geminiClientProvider.getIfAvailable()).thenReturn(mockChatClient);

        ChatClient routedClientNull = providerRouter.route(null);
        ChatClient routedClientBlank = providerRouter.route("   ");

        assertThat(routedClientNull).isSameAs(mockChatClient);
        assertThat(routedClientBlank).isSameAs(mockChatClient);
    }

    @Test
    void route_shouldThrowUnsupportedProviderWhenClientNotAvailable() {
        when(openAiClientProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> providerRouter.route("OPENAI"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.PROVIDER_NOT_CONFIGURED);
    }

    @Test
    void route_shouldThrowUnsupportedProviderWhenInvalidName() {
        assertThatThrownBy(() -> providerRouter.route("INVALID_PROVIDER"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.UNSUPPORTED_PROVIDER);
    }
}
