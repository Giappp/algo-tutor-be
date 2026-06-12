package org.rap.algotutorbe.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AdminLessonContentGenerateResponse;
import org.rap.algotutorbe.ai.services.AdminLessonContentGenerationService;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.dto.TheoryLessonRequestDTO;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminLessonContentControllerTest {

    @Mock
    private AdminLessonContentGenerationService generationService;

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminLessonContentController(generationService)).build();
    }

    @Test
    void generateContent_shouldReturnGeneratedDraft() throws Exception {
        TheoryLessonRequestDTO generated = new TheoryLessonRequestDTO();
        generated.setTitle("Array Basics");
        generated.setType(LessonType.THEORY);
        generated.setContent("Generated lesson");
        AdminLessonContentGenerateResponse response = new AdminLessonContentGenerateResponse(
                30L,
                LessonType.THEORY,
                generated,
                new AdminLessonContentGenerateResponse.GenerationContext(
                        10L, "Data Structures", 20L, "Arrays", List.of("Array Basics (THEORY)")),
                100,
                50);
        when(generationService.generate(eq(30L), any())).thenReturn(response);

        mockMvc.perform(post("/admin/ai/lessons/30/generate-content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"GEMINI\",\"prompt\":\"Rewrite for beginners\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lessonType").value("THEORY"))
                .andExpect(jsonPath("$.data.content.content").value("Generated lesson"));
    }

    @Test
    void generateContent_shouldRejectBlankPrompt() throws Exception {
        mockMvc.perform(post("/admin/ai/lessons/30/generate-content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
