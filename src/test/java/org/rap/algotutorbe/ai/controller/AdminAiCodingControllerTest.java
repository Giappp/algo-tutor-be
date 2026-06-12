package org.rap.algotutorbe.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AiQuestionSourceResponse;
import org.rap.algotutorbe.ai.services.AdminAiCodingService;
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
class AdminAiCodingControllerTest {

    @Mock
    private AdminAiCodingService service;
    @Mock
    private RateLimiter rateLimiter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAiCodingController(service, rateLimiter, 5, 60)).build();
    }

    @Test
    void getSources_shouldReturnSources() throws Exception {
        when(service.getSources(30L)).thenReturn(List.of(new AiQuestionSourceResponse(
                40L, "Hash Maps", 20L, "Hashing", 1, 10, 100, "Hash maps...", true)));

        mockMvc.perform(get("/admin/ai/coding-lessons/30/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lessonId").value(40));
    }

    @Test
    void generateProblem_shouldRejectInvalidCounts() throws Exception {
        mockMvc.perform(post("/admin/ai/coding-lessons/30/generate-problem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceLessonIds": [],
                                  "difficulty": "EASY",
                                  "exampleCount": 5,
                                  "hintCount": 4
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateStarterCode_shouldRejectEmptyLanguages() throws Exception {
        mockMvc.perform(post("/admin/ai/coding-lessons/30/generate-starter-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceLessonIds\":[],\"languages\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
