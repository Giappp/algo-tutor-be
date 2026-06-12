package org.rap.algotutorbe.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AiQuestionSourceResponse;
import org.rap.algotutorbe.ai.services.AdminAiQuizQuestionService;
import org.rap.algotutorbe.common.ratelimit.RateLimiter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAiQuizQuestionControllerTest {

    @Mock
    private AdminAiQuizQuestionService service;
    @Mock
    private RateLimiter rateLimiter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminAiQuizQuestionController(service, rateLimiter, 5, 60)).build();
    }

    @Test
    void getQuestionSources_shouldReturnSources() throws Exception {
        when(service.getQuestionSources(30L)).thenReturn(List.of(new AiQuestionSourceResponse(
                40L, "Binary Search", 20L, "Searching", 1, 10, 100,
                "Binary search halves...", true)));

        mockMvc.perform(get("/admin/ai/quiz-lessons/30/question-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lessonId").value(40))
                .andExpect(jsonPath("$.data[0].contentPreview").value("Binary search halves..."));
    }

    @Test
    void generateQuestions_shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/admin/ai/quiz-lessons/30/generate-questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceLessonIds": [],
                                  "difficulty": "MEDIUM",
                                  "questionTypes": [],
                                  "count": 11,
                                  "choicesPerQuestion": 1,
                                  "includeExplanations": true
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
