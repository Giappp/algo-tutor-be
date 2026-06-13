package org.rap.algotutorbe.ai.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.enums.AiGeneralChatIntent;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    @Value("classpath:/prompts/general_assistant.st")
    private Resource generalAssistantPromptResource;
    private String baseSystemPrompt = "";
    private String generalChatPrompt = "";
    private String generalAssistantPrompt = "";

    @PostConstruct
    public void init() {
        try {
            baseSystemPrompt = load(baseSystemPromptResource);
            generalChatPrompt = load(generalChatPromptResource);
            generalAssistantPrompt = load(generalAssistantPromptResource);

            modePrompts.clear();
            modePrompts.put(AiChatMode.GENERAL, "CHẾ ĐỘ: GENERAL");
            modePrompts.put(AiChatMode.HINT, load(hintPromptResource));
            modePrompts.put(AiChatMode.EXPLAIN, load(explainPromptResource));
            modePrompts.put(AiChatMode.DEBUG, load(debugPromptResource));
            modePrompts.put(AiChatMode.REVIEW, load(reviewPromptResource));
            modePrompts.put(AiChatMode.COMPLEXITY, load(complexityPromptResource));
            modePrompts.put(AiChatMode.SOLUTION, load(solutionPromptResource));
            modePrompts.put(AiChatMode.NEXT_STEP, load(nextStepPromptResource));
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
        return buildSystemPrompt(mode, true);
    }

    public String buildSystemPrompt(AiChatMode mode, boolean lessonCompleted) {
        String modeInstruction = modePrompts.getOrDefault(mode, "");
        return joinSections(baseSystemPrompt, modeInstruction, buildLessonDisclosurePolicy(mode, lessonCompleted));
    }

    /**
     * Builds the system prompt for the general academic advisor chatbot.
     */
    public String buildGeneralSystemPrompt() {
        return generalChatPrompt;
    }

    public String buildGeneralAssistantSystemPrompt(AiGeneralChatIntent intent) {
        String intentInstruction = switch (intent) {
            case CODING_HELP -> """
                    Ý định: CODING_HELP
                    - Ưu tiên nguyên nhân và hướng sửa ngắn gọn.
                    - Nếu thiếu dữ liệu, hỏi thêm code, input/output hoặc lỗi cụ thể.
                    """;
            case PLATFORM_HELP -> """
                    Ý định: PLATFORM_HELP
                    - Chỉ giải thích tính năng AlgoTutor đã biết chắc.
                    - Nếu không chắc, nói rõ và hướng học viên kiểm tra trên giao diện.
                    """;
            case GENERAL, ROADMAP_ADVISORY -> "";
        };

        return joinSections(generalAssistantPrompt, intentInstruction);
    }

    /**
     * Builds a structured user prompt assembling context, conversation history,
     * and the current request into labeled sections.
     */
    public String buildUserPrompt(AiChatRequest request, String context) {
        StringBuilder prompt = new StringBuilder();

        if (context != null && !context.isBlank()) {
            prompt.append(context.trim()).append("\n\n");
        }

        boolean hasCode = request.code() != null && !request.code().isBlank();
        if (hasCode) {
            prompt.append("[SUBMISSION_STATE]\n");
            if (request.language() != null && !request.language().isBlank()) {
                prompt.append("Ngôn ngữ: ").append(request.language()).append("\n");
            }
            prompt.append("Code của học viên:\n");
            prompt.append("```");
            if (request.language() != null && !request.language().isBlank()) {
                prompt.append(request.language());
            }
            prompt.append("\n").append(request.code()).append("\n```\n");
            prompt.append("[/SUBMISSION_STATE]\n\n");
        }

        prompt.append("[USER_QUERY]\n");
        prompt.append("Chế độ: ").append(request.mode()).append("\n");
        if (request.message() != null && !request.message().isBlank()) {
            prompt.append("Yêu cầu: ").append(request.message().trim()).append("\n");
        }
        prompt.append("[/USER_QUERY]");

        return prompt.toString();
    }

    private String buildLessonDisclosurePolicy(AiChatMode mode, boolean lessonCompleted) {
        if (mode == AiChatMode.SOLUTION) {
            return """
                    [DISCLOSURE_POLICY]
                    Được phép đưa lời giải hoàn chỉnh: nêu ý tưởng ngắn, code sạch, rồi độ phức tạp.
                    [/DISCLOSURE_POLICY]
                    """;
        }

        String completionState = lessonCompleted ? "đã hoàn thành" : "chưa hoàn thành";
        return """
                [DISCLOSURE_POLICY]
                Bài học: %s.
                Không đưa lời giải hoặc code hoàn chỉnh. Chỉ hướng dẫn, dry-run hoặc mã giả ngắn.
                [/DISCLOSURE_POLICY]
                """.formatted(completionState);
    }

    private String load(Resource resource) throws IOException {
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).trim();
    }

    private String joinSections(String... sections) {
        return Arrays.stream(sections)
                .filter(section -> section != null && !section.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }
}
