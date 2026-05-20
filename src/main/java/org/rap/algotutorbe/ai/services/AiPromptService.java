package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class AiPromptService {

    private static final String BASE_SYSTEM_PROMPT = """
            You are AlgoTutor AI, an intelligent algorithm and programming tutor for the AlgoTutor platform.
            
            Your role:
            - Help learners understand algorithms and data structures through guided learning.
            - Encourage independent thinking and problem-solving skills.
            - Respond in the same language the user uses (default to Vietnamese if unclear).
            - If you lack sufficient context, state clearly what information is missing rather than guessing.
            - Always be encouraging, patient, and pedagogically sound in your responses.
            
            Safety rules:
            - Never reveal full solutions unless explicitly in SOLUTION mode.
            - Never fabricate test cases or problem constraints that are not provided in context.
            - Keep responses focused on the algorithm/programming topic at hand.
            """;

    private final Map<AiChatMode, String> modePrompts;

    public AiPromptService() {
        this.modePrompts = new EnumMap<>(AiChatMode.class);
        initModePrompts();
    }

    private void initModePrompts() {
        modePrompts.put(AiChatMode.HINT, """
                MODE: HINT
                Instructions: Provide only a single hint of no more than 2 sentences. \
                Do NOT reveal the full solution, complete code, or detailed algorithm steps. \
                The hint should nudge the learner in the right direction without giving away the answer.""");

        modePrompts.put(AiChatMode.EXPLAIN, """
                MODE: EXPLAIN
                Instructions: Explain the relevant algorithm theory including its definition, \
                working principle, and an illustrative example. \
                Cover the core concept clearly so the learner understands the underlying theory. \
                Use step-by-step explanation with a concrete example to demonstrate how the algorithm works.""");

        modePrompts.put(AiChatMode.DEBUG, """
                MODE: DEBUG
                Instructions: Identify errors and edge cases in the user's code. \
                Provide fix guidance limited to pointing out the root cause of the issue. \
                Do NOT rewrite the full solution or provide complete corrected code. \
                Focus on what is wrong, why it is wrong, and give a minimal hint toward the fix.""");

        modePrompts.put(AiChatMode.REVIEW, """
                MODE: REVIEW
                Instructions: Review the user's code for correctness, style, and optimization opportunities. \
                Evaluate whether the logic is correct, identify any style issues or anti-patterns, \
                and suggest potential optimizations. Provide constructive feedback organized by category.""");

        modePrompts.put(AiChatMode.COMPLEXITY, """
                MODE: COMPLEXITY
                Instructions: Analyze and explain the time and space complexity of the user's code using Big-O notation. \
                Break down the analysis by identifying loops, recursive calls, and data structure operations. \
                Provide the final time complexity and space complexity with clear justification.""");

        modePrompts.put(AiChatMode.SOLUTION, """
                MODE: SOLUTION
                Instructions: Provide a complete algorithm solution with a step-by-step explanation of the approach. \
                Include the full working code along with detailed commentary on why each step is necessary. \
                Explain the algorithm choice, its complexity, and any trade-offs considered.""");

        modePrompts.put(AiChatMode.NEXT_STEP, """
                MODE: NEXT_STEP
                Instructions: Suggest the single next actionable step the learner should take to progress toward a solution. \
                Do NOT reveal subsequent steps or the full path to the solution. \
                The suggestion should be specific, concrete, and immediately actionable.""");
    }

    /**
     * Builds the complete system prompt by combining the base system prompt
     * with mode-specific instructions. Per Requirement 2.9, the mode-specific
     * instruction is always included regardless of conversation history.
     */
    public String buildSystemPrompt(AiChatMode mode) {
        String modeInstruction = modePrompts.getOrDefault(mode, "");
        return BASE_SYSTEM_PROMPT + "\n" + modeInstruction;
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
