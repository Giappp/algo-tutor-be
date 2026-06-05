package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProviderRouter {

    private final ObjectProvider<ChatClient> openAiClientProvider;
    private final ObjectProvider<ChatClient> geminiClientProvider;
    private final ObjectProvider<ChatClient> claudeClientProvider;
    private final LLMProvider defaultProvider;

    public ProviderRouter(
            @Qualifier("openAiChatClient") ObjectProvider<ChatClient> openAiClientProvider,
            @Qualifier("geminiChatClient") ObjectProvider<ChatClient> geminiClientProvider,
            @Qualifier("claudeChatClient") ObjectProvider<ChatClient> claudeClientProvider,
            LLMProvider defaultProvider) {
        this.openAiClientProvider = openAiClientProvider;
        this.geminiClientProvider = geminiClientProvider;
        this.claudeClientProvider = claudeClientProvider;
        this.defaultProvider = defaultProvider;
    }


    public ChatClient route(String providerName) {
        return routeByProvider(resolveProvider(providerName));
    }

    public ChatClient routeByProvider(LLMProvider provider) {
        if (provider == null) {
            provider = defaultProvider;
        }
        return switch (provider) {
            case OPENAI -> getRequiredClient(openAiClientProvider);
            case GEMINI -> getRequiredClient(geminiClientProvider);
            case CLAUDE -> getRequiredClient(claudeClientProvider);
        };
    }

    private ChatClient getRequiredClient(
            ObjectProvider<ChatClient> provider
    ) {
        ChatClient client = provider.getIfAvailable();

        if (client == null) {
            throw new AppException(ErrorCode.PROVIDER_NOT_CONFIGURED);
        }

        return client;
    }

    public LLMProvider resolveProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return defaultProvider;
        }

        try {
            return LLMProvider.valueOf(providerName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNSUPPORTED_PROVIDER);
        }
    }

    public List<LLMProvider> resolveFallbackChain(String providerName) {
        LLMProvider preferred = resolveProvider(providerName);
        List<LLMProvider> chain = new ArrayList<>();
        chain.add(preferred);

        for (LLMProvider provider : LLMProvider.values()) {
            if (provider != preferred) {
                chain.add(provider);
            }
        }

        return chain;
    }
}
