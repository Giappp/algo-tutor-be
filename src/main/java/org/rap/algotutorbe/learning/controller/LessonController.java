package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.LessonRequestDTO;
import org.rap.algotutorbe.learning.dto.landing.CodingContentResponse;
import org.rap.algotutorbe.learning.dto.landing.QuizContentResponse;
import org.rap.algotutorbe.learning.dto.landing.TheoryContentResponse;
import org.rap.algotutorbe.learning.dto.landing.VideoContentResponse;
import org.rap.algotutorbe.learning.services.LessonContentService;
import org.rap.algotutorbe.learning.services.LessonService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;
    private final LessonContentService lessonContentService;

    @PostMapping("/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> createLesson(
            @PathVariable Long topicId,
            @RequestBody @Valid LessonRequestDTO request) {
        return ResponseEntity.ok(lessonService.create(topicId, request));
    }

    @GetMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getLessonById(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getById(lessonId));
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getLessonBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(lessonService.getBySlug(slug));
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<ApiResponse<Object>> getLessonsByTopic(
            @PathVariable Long topicId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(lessonService.getByTopicId(topicId, pageable));
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> updateLesson(
            @PathVariable Long lessonId,
            @RequestBody @Valid LessonRequestDTO request) {
        return ResponseEntity.ok(lessonService.update(lessonId, request));
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.delete(lessonId));
    }

    @PatchMapping("/{lessonId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> togglePublish(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.togglePublish(lessonId));
    }

    @GetMapping("/{slug}/theory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TheoryContentResponse>> getTheoryContent(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(lessonContentService.getTheoryContent(slug)));
    }

    @GetMapping("/{slug}/quiz")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<QuizContentResponse>> getQuizContent(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(lessonContentService.getQuizContent(slug)));
    }

    @GetMapping("/{slug}/coding")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CodingContentResponse>> getCodingContent(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(lessonContentService.getCodingContent(slug)));
    }

    @GetMapping("/{slug}/video")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VideoContentResponse>> getVideoContent(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(lessonContentService.getVideoContent(slug)));
    }
}
