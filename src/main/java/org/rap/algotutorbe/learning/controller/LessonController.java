package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.LessonRequestDTO;
import org.rap.algotutorbe.learning.services.LessonService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;

    @PostMapping("/topics/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> createLesson(
            @PathVariable Long topicId,
            @RequestBody @Valid LessonRequestDTO request) {
        return ResponseEntity.ok(lessonService.create(topicId, request));
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<Object>> getLessonById(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getById(lessonId));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<Object>> getLessonBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(lessonService.getBySlug(slug));
    }

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<ApiResponse<Object>> getLessonsByTopic(
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "false") boolean publishedOnly) {
        return ResponseEntity.ok(lessonService.getByTopicId(topicId, publishedOnly));
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
    public ResponseEntity<ApiResponse<Object>> deleteLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.delete(lessonId));
    }

    @PatchMapping("/{lessonId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> togglePublish(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.togglePublish(lessonId));
    }
}
