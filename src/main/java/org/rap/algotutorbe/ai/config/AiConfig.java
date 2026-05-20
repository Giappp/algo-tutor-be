package org.rap.algotutorbe.ai.config;

import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI configuration that creates separate ChatClient beans for each LLM provider.
 * Each client is built from the auto-configured ChatModel beans.
 * Timeout is configured at 30 seconds via application properties.
 * The default provider is configurable via the ai.default-provider property.
 */
@Configuration
public class AiConfig {

    @Value("${ai.default-provider:GEMINI}")
    private String defaultProviderName;

    @Value("${ai.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    @ConditionalOnBean(OpenAiChatModel.class)
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @ConditionalOnBean(GoogleGenAiChatModel.class)
    public ChatClient geminiChatClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    @ConditionalOnBean(AnthropicChatModel.class)
    public ChatClient claudeChatClient(AnthropicChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public LLMProvider defaultProvider() {
        return LLMProvider.valueOf(defaultProviderName.toUpperCase());
    }

    @Bean
    public Duration aiRequestTimeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
