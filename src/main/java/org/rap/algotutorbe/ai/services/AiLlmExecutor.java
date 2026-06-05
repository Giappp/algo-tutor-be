package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiLlmExecutor {

    private final ProviderRouter providerRouter;

    public ChatResponseWithTokens callWithFallback(
            String providerName,
            List<Message> messages,
            Object tools) {
        AppException lastFailure = null;

        for (LLMProvider provider : providerRouter.resolveFallbackChain(providerName)) {
            try {
                return call(provider, messages, tools);
            } catch (AppException e) {
                lastFailure = e;
                log.warn("LLM provider [{}] failed; trying fallback if available", provider, e);
            }
        }

        throw lastFailure != null ? lastFailure : new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    public Disposable streamWithFallback(
            String providerName,
            List<Message> messages,
            Object tools,
            Consumer<String> chunkHandler,
            Consumer<ChatResponseWithTokens> completionHandler,
            Consumer<Throwable> errorHandler) {
        AppException lastFailure = null;

        for (LLMProvider provider : providerRouter.resolveFallbackChain(providerName)) {
            try {
                return stream(provider, messages, tools, chunkHandler, completionHandler, errorHandler);
            } catch (AppException e) {
                lastFailure = e;
                log.warn("Failed to start LLM stream for provider [{}]; trying fallback if available", provider, e);
            }
        }

        AppException failure = lastFailure != null ? lastFailure : new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        errorHandler.accept(failure);
        return () -> {
        };
    }

    private ChatResponseWithTokens call(LLMProvider provider, List<Message> messages, Object tools) {
        try {
            ChatClient chatClient = providerRouter.routeByProvider(provider);
            ChatResponse chatResponse = tools == null
                    ? chatClient.prompt().messages(messages).call().chatResponse()
                    : chatClient.prompt().messages(messages).tools(tools).call().chatResponse();

            String responseText = extractRequiredResponseText(chatResponse);
            TokenUsage tokenUsage = extractTokenUsage(chatResponse);
            return new ChatResponseWithTokens(responseText, tokenUsage.inputTokens(), tokenUsage.outputTokens());
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            logLlmException(provider, e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, e);
        }
    }

    private Disposable stream(
            LLMProvider provider,
            List<Message> messages,
            Object tools,
            Consumer<String> chunkHandler,
            Consumer<ChatResponseWithTokens> completionHandler,
            Consumer<Throwable> errorHandler) {
        try {
            ChatClient chatClient = providerRouter.routeByProvider(provider);
            Flux<ChatResponse> stream = tools == null
                    ? chatClient.prompt().messages(messages).stream().chatResponse()
                    : chatClient.prompt().messages(messages).tools(tools).stream().chatResponse();

            StreamAccumulator accumulator = new StreamAccumulator();
            return stream.subscribe(
                    chatResponse -> handleStreamChunk(chatResponse, accumulator, chunkHandler),
                    error -> {
                        log.error("LLM stream error for provider [{}]", provider, error);
                        errorHandler.accept(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, error));
                    },
                    () -> completionHandler.accept(accumulator.toChatResponseWithTokens()));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            logLlmException(provider, e);
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE, e);
        }
    }

    private void handleStreamChunk(
            ChatResponse chatResponse,
            StreamAccumulator accumulator,
            Consumer<String> chunkHandler) {
        if (chatResponse == null) {
            return;
        }

        accumulator.updateUsage(extractTokenUsage(chatResponse));
        String chunkText = extractNullableResponseText(chatResponse);
        if (chunkText == null) {
            return;
        }

        accumulator.append(chunkText);
        chunkHandler.accept(chunkText);
    }

    private String extractRequiredResponseText(ChatResponse chatResponse) {
        String responseText = extractNullableResponseText(chatResponse);

        if (responseText == null || responseText.isBlank()) {
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        return responseText;
    }

    private String extractNullableResponseText(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return null;
        }

        return chatResponse.getResult().getOutput().getText();
    }

    private TokenUsage extractTokenUsage(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null || chatResponse.getMetadata().getUsage() == null) {
            return TokenUsage.empty();
        }

        Usage usage = chatResponse.getMetadata().getUsage();
        return new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens());
    }

    private void logLlmException(LLMProvider provider, Exception e) {
        if (isTimeoutException(e)) {
            log.error("LLM call timed out for provider [{}]", provider, e);
            return;
        }

        log.error("LLM provider error for provider [{}]", provider, e);
    }

    private boolean isTimeoutException(Exception e) {
        Throwable current = e;

        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof TimeoutException) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && isTimeoutMessage(message)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private boolean isTimeoutMessage(String message) {
        String normalizedMessage = message.toLowerCase();
        return normalizedMessage.contains("timed out")
                || normalizedMessage.contains("timeout")
                || normalizedMessage.contains("read timed out");
    }

    private record TokenUsage(Integer inputTokens, Integer outputTokens) {
        static TokenUsage empty() {
            return new TokenUsage(null, null);
        }
    }

    private static class StreamAccumulator {
        private final StringBuilder responseText = new StringBuilder();
        private Integer inputTokens;
        private Integer outputTokens;

        void append(String chunkText) {
            responseText.append(chunkText);
        }

        void updateUsage(TokenUsage tokenUsage) {
            if (tokenUsage.inputTokens() != null) {
                inputTokens = tokenUsage.inputTokens();
            }
            if (tokenUsage.outputTokens() != null) {
                outputTokens = tokenUsage.outputTokens();
            }
        }

        ChatResponseWithTokens toChatResponseWithTokens() {
            return new ChatResponseWithTokens(responseText.toString(), inputTokens, outputTokens);
        }
    }

    public record ChatResponseWithTokens(String responseText, Integer inputTokens, Integer outputTokens) {
    }
}
