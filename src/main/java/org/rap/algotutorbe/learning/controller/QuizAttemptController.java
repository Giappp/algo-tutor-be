package org.rap.algotutorbe.learning.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptSummary;
import org.rap.algotutorbe.learning.dto.quiz.QuizSubmitAnswer;
import org.rap.algotutorbe.learning.services.QuizAttemptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizAttemptController {
    private final QuizAttemptService quizAttemptService;

    @PostMapping("/{lessonSlug}/quiz-attempts")
    public ResponseEntity<ApiResponse<Object>> submitQuiz(@PathVariable String lessonSlug, @RequestBody QuizSubmitAnswer payload,
                                                          @AuthenticationPrincipal SecurityUser authentication) {
        var response = quizAttemptService.submitQuiz(lessonSlug, payload, authentication);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @GetMapping("/{lessonSlug}/quiz-attempts")
    public ResponseEntity<ApiResponse<List<QuizAttemptSummary>>> getAllQuizAttempt(@PathVariable String lessonSlug,
                                                                                   @AuthenticationPrincipal SecurityUser authentication) {
        var response = quizAttemptService.getAllQuizSummary(lessonSlug, authentication);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }
}
