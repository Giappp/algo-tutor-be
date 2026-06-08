package org.rap.algotutorbe.ai.config;

import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${ai.default-provider:GEMINI}")
    private String defaultProviderName;

    @Value("${ai.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("geminiChatClient")
    public ChatClient geminiChatClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean("claudeChatClient")
    public ChatClient claudeChatClient(AnthropicChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public LLMProvider defaultProvider() {
        try {
            return LLMProvider.valueOf(defaultProviderName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid ai.default-provider: " + defaultProviderName, e);
        }
    }

    @Bean
    public Duration aiRequestTimeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }
}
