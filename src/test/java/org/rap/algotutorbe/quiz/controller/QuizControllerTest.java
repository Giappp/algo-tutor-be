package org.rap.algotutorbe.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.quiz.dto.QuizAttemptResponse;
import org.rap.algotutorbe.quiz.dto.QuizResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
class QuizControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    QuizService quizService;

    @MockBean
    QuizMapper quizMapper;

    @Test
    void getQuizByLessonId_returnsQuiz() throws Exception {
        QuizResponse quiz = new QuizResponse(
                1L, "Arrays Quiz", "Test your array knowledge",
                70, 15, 5, 5, List.of()
        );
        when(quizService.getQuizByLessonId(1L))
                .thenReturn(ApiResponse.buildSuccess(quiz));

        mvc.perform(get("/learning-paths/lessons/1/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Arrays Quiz"))
                .andExpect(jsonPath("$.data.passingScore").value(70))
                .andExpect(jsonPath("$.data.questionCount").value(5));
    }

    @Test
    void getQuizAttempts_returnsAttempts() throws Exception {
        UUID attemptId = UUID.randomUUID();
        QuizAttemptResponse attempt = new QuizAttemptResponse(
                attemptId, 1L, "Arrays Quiz", 80, 100,
                true, Instant.now(), Instant.now(), 300
        );
        when(quizService.getAttemptsByLessonId(1L))
                .thenReturn(ApiResponse.buildSuccess(List.of(attempt)));

        mvc.perform(get("/learning-paths/lessons/1/quiz/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].score").value(80))
                .andExpect(jsonPath("$.data[0].passed").value(true));
    }
}
