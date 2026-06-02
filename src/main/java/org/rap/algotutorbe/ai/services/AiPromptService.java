package org.rap.algotutorbe.ai.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Service
public class AiPromptService {

    private final Map<AiChatMode, String> modePrompts = new EnumMap<>(AiChatMode.class);
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
    @Value("classpath:/prompts/general_chat.st")
    private Resource generalChatPromptResource;
    private String baseSystemPrompt = "";
    private String generalChatPrompt = "";

    @PostConstruct
    public void init() {
        try {
            baseSystemPrompt = StreamUtils.copyToString(baseSystemPromptResource.getInputStream(), StandardCharsets.UTF_8);
            generalChatPrompt = StreamUtils.copyToString(generalChatPromptResource.getInputStream(), StandardCharsets.UTF_8);
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
            throw new AppException(ErrorCode.LOAD_TEMPLATE_FAILED, e);
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
     * Builds the system prompt for the general academic advisor chatbot.
     */
    public String buildGeneralSystemPrompt() {
        return generalChatPrompt;
    }

    /**
     * Builds a structured user prompt assembling context, conversation history,
     * and the current request into labeled sections.
     */
    public String buildUserPrompt(AiChatRequest request, String context) {
        StringBuilder prompt = new StringBuilder();

        // 1. Context Section (contains lesson instructions and judge compile results)
        if (context != null && !context.isBlank()) {
            prompt.append(context);
        }

        // 2. Submission State Section
        boolean hasCode = request.code() != null && !request.code().isBlank();
        if (hasCode) {
            prompt.append("[SUBMISSION_STATE]\n");
            if (request.language() != null && !request.language().isBlank()) {
                prompt.append("Programming language: ").append(request.language()).append("\n");
            }
            prompt.append("User code:\n");
            prompt.append("```");
            if (request.language() != null && !request.language().isBlank()) {
                prompt.append(request.language());
            }
            prompt.append("\n").append(request.code()).append("\n```\n");
            prompt.append("[/SUBMISSION_STATE]\n\n");
        }

        // 3. User Query Section
        prompt.append("[USER_QUERY]\n");
        prompt.append("Mode: ").append(request.mode()).append("\n");
        if (request.message() != null && !request.message().isBlank()) {
            prompt.append("User message: ").append(request.message()).append("\n");
        }
        prompt.append("[/USER_QUERY]");

        return prompt.toString();
    }
}
