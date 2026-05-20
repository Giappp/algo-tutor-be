package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.enums.AiChatMode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptServiceTest {

    private AiPromptService promptService;

    @BeforeEach
    void setUp() {
        promptService = new AiPromptService();
    }

    // --- buildSystemPrompt tests ---

    @ParameterizedTest
    @EnumSource(AiChatMode.class)
    void buildSystemPrompt_allModes_containsBasePrompt(AiChatMode mode) {
        String result = promptService.buildSystemPrompt(mode);
        assertThat(result).contains("AlgoTutor AI");
    }

    @ParameterizedTest
    @EnumSource(AiChatMode.class)
    void buildSystemPrompt_allModes_containsModeInstruction(AiChatMode mode) {
        String result = promptService.buildSystemPrompt(mode);
        assertThat(result).contains("MODE: " + mode.name());
    }

    @Test
    void buildSystemPrompt_hintMode_containsHintConstraints() {
        String result = promptService.buildSystemPrompt(AiChatMode.HINT);
        assertThat(result)
                .contains("single hint")
                .contains("2 sentences")
                .contains("Do NOT reveal the full solution");
    }

    @Test
    void buildSystemPrompt_explainMode_containsExplainInstructions() {
        String result = promptService.buildSystemPrompt(AiChatMode.EXPLAIN);
        assertThat(result)
                .contains("algorithm theory")
                .contains("definition")
                .contains("working principle")
                .contains("example");
    }

    @Test
    void buildSystemPrompt_debugMode_containsDebugConstraints() {
        String result = promptService.buildSystemPrompt(AiChatMode.DEBUG);
        assertThat(result)
                .contains("Identify errors")
                .contains("edge cases")
                .contains("root cause")
                .contains("Do NOT rewrite the full solution");
    }

    @Test
    void buildSystemPrompt_reviewMode_containsReviewInstructions() {
        String result = promptService.buildSystemPrompt(AiChatMode.REVIEW);
        assertThat(result)
                .contains("correctness")
                .contains("style")
                .contains("optimization");
    }

    @Test
    void buildSystemPrompt_complexityMode_containsComplexityInstructions() {
        String result = promptService.buildSystemPrompt(AiChatMode.COMPLEXITY);
        assertThat(result)
                .contains("time and space complexity")
                .contains("Big-O notation");
    }

    @Test
    void buildSystemPrompt_solutionMode_containsSolutionInstructions() {
        String result = promptService.buildSystemPrompt(AiChatMode.SOLUTION);
        assertThat(result)
                .contains("complete algorithm solution")
                .contains("step-by-step explanation");
    }

    @Test
    void buildSystemPrompt_nextStepMode_containsNextStepConstraints() {
        String result = promptService.buildSystemPrompt(AiChatMode.NEXT_STEP);
        assertThat(result)
                .contains("single next actionable step")
                .contains("Do NOT reveal subsequent steps");
    }

    // --- buildUserPrompt tests ---

    @Test
    void buildUserPrompt_withAllFields_containsAllSections() {
        AiChatRequest request = new AiChatRequest(
                null, null, null, null, "HINT",
                "How do I solve this?", "int x = 1;", "java",
                null, null, null
        );
        String context = "[LESSON_CONTEXT]\nTwo Sum problem\n[/LESSON_CONTEXT]";
        String history = "User: Hello\nAssistant: Hi!";

        String result = promptService.buildUserPrompt(request, context, history);

        assertThat(result)
                .contains("[CONTEXT]")
                .contains("Two Sum problem")
                .contains("[/CONTEXT]")
                .contains("[CONVERSATION_HISTORY]")
                .contains("User: Hello")
                .contains("[/CONVERSATION_HISTORY]")
                .contains("[CURRENT_REQUEST]")
                .contains("Mode: HINT")
                .contains("User message: How do I solve this?")
                .contains("```java")
                .contains("int x = 1;")
                .contains("[/CURRENT_REQUEST]");
    }

    @Test
    void buildUserPrompt_withNullContext_omitsContextSection() {
        AiChatRequest request = new AiChatRequest(
                null, null, null, null, "EXPLAIN",
                "What is BFS?", null, null,
                null, null, null
        );

        String result = promptService.buildUserPrompt(request, null, null);

        assertThat(result)
                .doesNotContain("[CONTEXT]")
                .doesNotContain("[CONVERSATION_HISTORY]")
                .contains("[CURRENT_REQUEST]")
                .contains("Mode: EXPLAIN")
                .contains("User message: What is BFS?");
    }

    @Test
    void buildUserPrompt_withBlankContext_omitsContextSection() {
        AiChatRequest request = new AiChatRequest(
                null, null, null, null, "EXPLAIN",
                "What is BFS?", null, null,
                null, null, null
        );

        String result = promptService.buildUserPrompt(request, "   ", "");

        assertThat(result)
                .doesNotContain("[CONTEXT]")
                .doesNotContain("[CONVERSATION_HISTORY]");
    }

    @Test
    void buildUserPrompt_withCodeButNoLanguage_includesCodeWithoutAnnotation() {
        AiChatRequest request = new AiChatRequest(
                null, null, null, null, "DEBUG",
                null, "def foo(): pass", null,
                null, null, null
        );

        String result = promptService.buildUserPrompt(request, null, null);

        assertThat(result)
                .contains("```\ndef foo(): pass\n```")
                .doesNotContain("Programming language:");
    }

    @Test
    void buildUserPrompt_withCodeAndLanguage_includesLanguageAnnotation() {
        AiChatRequest request = new AiChatRequest(
                null, null, null, null, "REVIEW",
                null, "print('hello')", "python",
                null, null, null
        );

        String result = promptService.buildUserPrompt(request, null, null);

        assertThat(result)
                .contains("```python")
                .contains("print('hello')")
                .contains("Programming language: python");
    }

    @Test
    void buildUserPrompt_withOnlyMessage_noCodeSection() {
        AiChatRequest request = new AiChatRequest(
                null, null, null, null, "HINT",
                "Give me a hint", null, null,
                null, null, null
        );

        String result = promptService.buildUserPrompt(request, null, null);

        assertThat(result)
                .contains("User message: Give me a hint")
                .doesNotContain("User code:");
    }
}
