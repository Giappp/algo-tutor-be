package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.ai.enums.AiChatMode;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptServiceTest {

    private final AiPromptService aiPromptService = new AiPromptService();

    @Test
    void buildSystemPrompt_shouldIncludeModeInstructions() {
        for (AiChatMode mode : AiChatMode.values()) {
            String prompt = aiPromptService.buildSystemPrompt(mode);
            assertThat(prompt).contains("You are AlgoTutor AI");
            assertThat(prompt).contains("MODE: " + mode.name());
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
        String history = "User: Hello\nAssistant: Hi";

        String userPrompt = aiPromptService.buildUserPrompt(request, context, history);

        assertThat(userPrompt).contains("[CONTEXT]");
        assertThat(userPrompt).contains("Lesson Context Title: Two Sum");
        assertThat(userPrompt).contains("[CONVERSATION_HISTORY]");
        assertThat(userPrompt).contains("User: Hello");
        assertThat(userPrompt).contains("[CURRENT_REQUEST]");
        assertThat(userPrompt).contains("Mode: HINT");
        assertThat(userPrompt).contains("User message: How to solve this?");
        assertThat(userPrompt).contains("User code:");
        assertThat(userPrompt).contains("```java");
        assertThat(userPrompt).contains("int a = 5;");
    }
}
