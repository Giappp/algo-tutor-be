package org.rap.algotutorbe.ai.services;

import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiSuggestion;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates contextual follow-up suggestions based on the current chat mode and lesson type.
 * Suggestions are deterministic (no LLM call needed) and guide the learner toward
 * productive next steps in their learning journey.
 */
@Slf4j
@Component
public class SuggestionGenerator {

    private static final String LESSON_TYPE_CODING = "CodingLesson";
    private static final String LESSON_TYPE_THEORY = "TheoryLesson";

    /**
     * Generates 2-4 contextual follow-up suggestions based on the current mode and lesson type.
     * Each suggestion contains a label (max 100 chars), mode (valid AiChatMode), and message (max 500 chars).
     *
     * @param currentMode the current chat mode
     * @param aiResponse  the AI response content (used for context, may be null)
     * @param lessonType  the type of lesson ("CodingLesson" or "TheoryLesson", may be null)
     * @return a list of 2-4 suggestions, or an empty list if generation fails
     */
    public List<AiSuggestion> generate(AiChatMode currentMode, String aiResponse, String lessonType) {
        try {
            return buildSuggestionsForMode(currentMode, lessonType);
        } catch (Exception e) {
            log.warn("Failed to generate suggestions for mode={}, lessonType={}: {}",
                    currentMode, lessonType, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Determines whether the user can ask for the next hint.
     * Returns true if the number of ASSISTANT HINT messages in the conversation
     * is fewer than the total available hints for the lesson.
     *
     * @param hintCount the number of ASSISTANT messages with mode HINT in the current conversation
     * @param maxHints  the total number of available hints for the associated coding lesson
     * @return true if more hints are available, false otherwise
     */
    public boolean canAskNextHint(int hintCount, int maxHints) {
        return hintCount < maxHints;
    }

    public List<AiSuggestion> buildSuggestionsForMode(AiChatMode currentMode, String lessonType) {
        return switch (currentMode) {
            case HINT -> buildHintModeSuggestions(lessonType);
            case EXPLAIN -> buildExplainModeSuggestions(lessonType);
            case DEBUG -> buildDebugModeSuggestions(lessonType);
            case REVIEW -> buildReviewModeSuggestions(lessonType);
            case COMPLEXITY -> buildComplexityModeSuggestions(lessonType);
            case SOLUTION -> buildSolutionModeSuggestions(lessonType);
            case NEXT_STEP -> buildNextStepModeSuggestions(lessonType);
        };
    }

    private List<AiSuggestion> buildHintModeSuggestions(String lessonType) {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                "Ask for another hint",
                AiChatMode.HINT.name(),
                "Can you give me another hint to help me progress further?"
        ));
        suggestions.add(new AiSuggestion(
                "Explain the algorithm",
                AiChatMode.EXPLAIN.name(),
                "Can you explain the algorithm concept behind this problem?"
        ));
        suggestions.add(new AiSuggestion(
                "Show me the solution",
                AiChatMode.SOLUTION.name(),
                "Please show me the complete solution with explanation."
        ));
        if (isCodingLesson(lessonType)) {
            suggestions.add(new AiSuggestion(
                    "What should I do next?",
                    AiChatMode.NEXT_STEP.name(),
                    "What is the next step I should take to solve this problem?"
            ));
        }
        return suggestions;
    }

    private List<AiSuggestion> buildExplainModeSuggestions(String lessonType) {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                "Give me a hint",
                AiChatMode.HINT.name(),
                "Can you give me a hint on how to implement this?"
        ));
        suggestions.add(new AiSuggestion(
                "Show me the solution",
                AiChatMode.SOLUTION.name(),
                "Please show me the complete solution with step-by-step explanation."
        ));
        if (isCodingLesson(lessonType)) {
            suggestions.add(new AiSuggestion(
                    "Analyze complexity",
                    AiChatMode.COMPLEXITY.name(),
                    "What is the time and space complexity of this algorithm?"
            ));
            suggestions.add(new AiSuggestion(
                    "What should I do next?",
                    AiChatMode.NEXT_STEP.name(),
                    "What is the next step I should take to solve this problem?"
            ));
        } else {
            suggestions.add(new AiSuggestion(
                    "Explain further",
                    AiChatMode.EXPLAIN.name(),
                    "Can you explain this concept in more detail with another example?"
            ));
        }
        return suggestions;
    }

    private List<AiSuggestion> buildDebugModeSuggestions(String lessonType) {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                "Review my code",
                AiChatMode.REVIEW.name(),
                "Can you review my code for correctness, style, and optimization?"
        ));
        suggestions.add(new AiSuggestion(
                "Analyze complexity",
                AiChatMode.COMPLEXITY.name(),
                "What is the time and space complexity of my code?"
        ));
        suggestions.add(new AiSuggestion(
                "Give me a hint",
                AiChatMode.HINT.name(),
                "Can you give me a hint on how to fix the issue?"
        ));
        suggestions.add(new AiSuggestion(
                "What should I do next?",
                AiChatMode.NEXT_STEP.name(),
                "What is the next step I should take to fix this bug?"
        ));
        return suggestions;
    }

    private List<AiSuggestion> buildReviewModeSuggestions(String lessonType) {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                "Analyze complexity",
                AiChatMode.COMPLEXITY.name(),
                "What is the time and space complexity of my code?"
        ));
        suggestions.add(new AiSuggestion(
                "Debug my code",
                AiChatMode.DEBUG.name(),
                "Can you help me identify and fix errors in my code?"
        ));
        suggestions.add(new AiSuggestion(
                "Show me the optimal solution",
                AiChatMode.SOLUTION.name(),
                "Can you show me the optimal solution for this problem?"
        ));
        return suggestions;
    }

    private List<AiSuggestion> buildComplexityModeSuggestions(String lessonType) {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                "Review my code",
                AiChatMode.REVIEW.name(),
                "Can you review my code for correctness and style improvements?"
        ));
        suggestions.add(new AiSuggestion(
                "Show me the optimal solution",
                AiChatMode.SOLUTION.name(),
                "Can you show me a more optimal solution with better complexity?"
        ));
        suggestions.add(new AiSuggestion(
                "Explain the algorithm",
                AiChatMode.EXPLAIN.name(),
                "Can you explain the algorithm and why it has this complexity?"
        ));
        return suggestions;
    }

    private List<AiSuggestion> buildSolutionModeSuggestions(String lessonType) {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                "Explain the approach",
                AiChatMode.EXPLAIN.name(),
                "Can you explain the approach and algorithm used in this solution?"
        ));
        suggestions.add(new AiSuggestion(
                "Analyze complexity",
                AiChatMode.COMPLEXITY.name(),
                "What is the time and space complexity of this solution?"
        ));
        if (isCodingLesson(lessonType)) {
            suggestions.add(new AiSuggestion(
                    "Give me a hint for another approach",
                    AiChatMode.HINT.name(),
                    "Can you give me a hint about an alternative approach to solve this?"
            ));
        }
        return suggestions;
    }

    private List<AiSuggestion> buildNextStepModeSuggestions(String lessonType) {
        List<AiSuggestion> suggestions = new ArrayList<>();
        suggestions.add(new AiSuggestion(
                "Give me a hint",
                AiChatMode.HINT.name(),
                "Can you give me a hint to help with this step?"
        ));
        suggestions.add(new AiSuggestion(
                "Explain the concept",
                AiChatMode.EXPLAIN.name(),
                "Can you explain the concept behind this step?"
        ));
        suggestions.add(new AiSuggestion(
                "Show me the solution",
                AiChatMode.SOLUTION.name(),
                "Please show me the complete solution with explanation."
        ));
        return suggestions;
    }

    private boolean isCodingLesson(String lessonType) {
        return LESSON_TYPE_CODING.equals(lessonType);
    }
}
