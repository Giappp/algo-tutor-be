package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Service
public class AiPromptService {

    @Value("classpath:/prompts/base_system.st")
    private Resource baseSystemPromptResource;

    @Value("classpath:/prompts/hint.st")
    private Resource hintPromptResource;

    @Value("classpath:/prompts/explain.st")
    private Resource explainPromptResource;

    @Value("classpath:/prompts/debug.st")
    private Resource debugPromptResource;

    @Value("classpath:/prompts/review.st")
    private Resource reviewPromptResource;

    @Value("classpath:/prompts/complexity.st")
    private Resource complexityPromptResource;

    @Value("classpath:/prompts/solution.st")
    private Resource solutionPromptResource;

    @Value("classpath:/prompts/next_step.st")
    private Resource nextStepPromptResource;

    private String baseSystemPrompt = "";
    private final Map<AiChatMode, String> modePrompts = new EnumMap<>(AiChatMode.class);

    @PostConstruct
    public void init() {
        try {
            baseSystemPrompt = StreamUtils.copyToString(baseSystemPromptResource.getInputStream(), StandardCharsets.UTF_8);
            modePrompts.put(AiChatMode.HINT, StreamUtils.copyToString(hintPromptResource.getInputStream(), StandardCharsets.UTF_8));
            modePrompts.put(AiChatMode.EXPLAIN, StreamUtils.copyToString(explainPromptResource.getInputStream(), StandardCharsets.UTF_8));
            modePrompts.put(AiChatMode.DEBUG, StreamUtils.copyToString(debugPromptResource.getInputStream(), StandardCharsets.UTF_8));
            modePrompts.put(AiChatMode.REVIEW, StreamUtils.copyToString(reviewPromptResource.getInputStream(), StandardCharsets.UTF_8));
            modePrompts.put(AiChatMode.COMPLEXITY, StreamUtils.copyToString(complexityPromptResource.getInputStream(), StandardCharsets.UTF_8));
            modePrompts.put(AiChatMode.SOLUTION, StreamUtils.copyToString(solutionPromptResource.getInputStream(), StandardCharsets.UTF_8));
            modePrompts.put(AiChatMode.NEXT_STEP, StreamUtils.copyToString(nextStepPromptResource.getInputStream(), StandardCharsets.UTF_8));
            log.info("AI Prompt templates loaded successfully from resources.");
        } catch (IOException e) {
            log.error("Failed to load prompt templates from resources", e);
            throw new RuntimeException("Initialization of AI Prompts failed", e);
        }
    }

    /**
     * Builds the complete system prompt by combining the base system prompt
     * with mode-specific instructions.
     */
    public String buildSystemPrompt(AiChatMode mode) {
        String modeInstruction = modePrompts.getOrDefault(mode, "");
        return baseSystemPrompt + "\n" + modeInstruction;
    }

    /**
     * Builds a structured user prompt assembling context, conversation history,
     * and the current request into labeled sections.
     */
    public String buildUserPrompt(AiChatRequest request, String context, String history) {
        StringBuilder prompt = new StringBuilder();

        // Context section
        if (context != null && !context.isBlank()) {
            prompt.append("[CONTEXT]\n");
            prompt.append(context);
            prompt.append("\n[/CONTEXT]\n\n");
        }

        // Conversation history section
        if (history != null && !history.isBlank()) {
            prompt.append("[CONVERSATION_HISTORY]\n");
            prompt.append(history);
            prompt.append("\n[/CONVERSATION_HISTORY]\n\n");
        }

        // Current request section
        prompt.append("[CURRENT_REQUEST]\n");
        prompt.append("Mode: ").append(request.mode()).append("\n");

        if (request.message() != null && !request.message().isBlank()) {
            prompt.append("User message: ").append(request.message()).append("\n");
        }

        boolean isProvideCode = request.language() != null && !request.language().isBlank();
        if (request.code() != null && !request.code().isBlank()) {
            prompt.append("User code:\n");
            if (isProvideCode) {
                prompt.append("```").append(request.language()).append("\n");
            } else {
                prompt.append("```\n");
            }
            prompt.append(request.code()).append("\n");
            prompt.append("```\n");
        }

        if (isProvideCode) {
            prompt.append("Programming language: ").append(request.language()).append("\n");
        }

        prompt.append("[/CURRENT_REQUEST]");

        return prompt.toString();
    }
}
