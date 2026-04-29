package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.QuizQuestionDTO;
import org.rap.algotutorbe.learning.services.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuizQuestionController {
    private final QuizService quizService;

    @PostMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> addQuestion(
            @PathVariable Long lessonId,
            @RequestBody @Valid QuizQuestionDTO request) {
        return ResponseEntity.ok(quizService.addQuestion(lessonId, request));
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<Object>> getQuestionsByQuizLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(quizService.getQuestionsByQuizLessonId(lessonId));
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody @Valid QuizQuestionDTO request) {
        return ResponseEntity.ok(quizService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteQuestion(@PathVariable Long questionId) {
        return ResponseEntity.ok(quizService.deleteQuestion(questionId));
    }
}
