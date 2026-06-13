package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptServiceTest {

    private final AiPromptService aiPromptService = new AiPromptService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiPromptService, "baseSystemPromptResource", resource("You are AlgoTutor AI"));
        ReflectionTestUtils.setField(aiPromptService, "generalChatPromptResource", resource("General"));
        ReflectionTestUtils.setField(aiPromptService, "generalAssistantPromptResource", resource("General assistant"));
        ReflectionTestUtils.setField(aiPromptService, "hintPromptResource", resource("CHẾ ĐỘ: HINT"));
        ReflectionTestUtils.setField(aiPromptService, "explainPromptResource", resource("CHẾ ĐỘ: EXPLAIN"));
        ReflectionTestUtils.setField(aiPromptService, "debugPromptResource", resource("CHẾ ĐỘ: DEBUG"));
        ReflectionTestUtils.setField(aiPromptService, "reviewPromptResource", resource("CHẾ ĐỘ: REVIEW"));
        ReflectionTestUtils.setField(aiPromptService, "complexityPromptResource", resource("CHẾ ĐỘ: COMPLEXITY"));
        ReflectionTestUtils.setField(aiPromptService, "solutionPromptResource", resource("CHẾ ĐỘ: SOLUTION"));
        ReflectionTestUtils.setField(aiPromptService, "nextStepPromptResource", resource("CHẾ ĐỘ: NEXT_STEP"));
        aiPromptService.init();
    }

    private ByteArrayResource resource(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void buildGeneralAssistantSystemPrompt_shouldLoadTemplateAndIntentInstructions() {
        String prompt = aiPromptService.buildGeneralAssistantSystemPrompt(
                org.rap.algotutorbe.ai.enums.AiGeneralChatIntent.CODING_HELP);

        assertThat(prompt).contains("General assistant");
        assertThat(prompt).contains("CODING_HELP");
    }

    @Test
    void buildSystemPrompt_shouldIncludeModeInstructions() {
        for (AiChatMode mode : AiChatMode.values()) {
            String prompt = aiPromptService.buildSystemPrompt(mode);
            assertThat(prompt).contains("You are AlgoTutor AI");
            assertThat(prompt).contains("CHẾ ĐỘ: " + mode.name());
        }
    }

    @Test
    void buildUserPrompt_shouldAssembleAllSections() {
        AiChatRequest request = new AiChatRequest(
                null,
                1L,
                "two-sum",
                "GEMINI",
                "HINT",
                "How to solve this?",
                "int a = 5;",
                "java",
                null,
                null,
                Collections.emptyList()
        );

        String context = "Lesson Context Title: Two Sum";

        String userPrompt = aiPromptService.buildUserPrompt(request, context);

        assertThat(userPrompt).contains("Lesson Context Title: Two Sum");
        assertThat(userPrompt).contains("[SUBMISSION_STATE]");
        assertThat(userPrompt).contains("[USER_QUERY]");
        assertThat(userPrompt).contains("Chế độ: HINT");
        assertThat(userPrompt).contains("Yêu cầu: How to solve this?");
        assertThat(userPrompt).contains("Code của học viên:");
        assertThat(userPrompt).contains("```java");
        assertThat(userPrompt).contains("int a = 5;");
    }
}
