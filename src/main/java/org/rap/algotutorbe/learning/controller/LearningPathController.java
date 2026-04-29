package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.dto.LearningPathResponseDTO;
import org.rap.algotutorbe.learning.enums.Level;
import org.rap.algotutorbe.learning.services.LearningPathService;
import org.rap.algotutorbe.learning.services.TopicService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/learning-paths")
@RequiredArgsConstructor
public class LearningPathController {
    private final LearningPathService learningPathService;
    private final TopicService topicService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getAllLearningPaths(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(learningPathService.getAll(pageable, level, search));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> createLearningPath(@RequestBody @Valid LearningPathRequestDTO request) {
        return ResponseEntity.ok(learningPathService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> getLearningPathById(@PathVariable Long id) {
        return ResponseEntity.ok(learningPathService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> updateLearningPath(@PathVariable Long id, @RequestBody @Valid LearningPathRequestDTO request) {
        return ResponseEntity.ok(learningPathService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteLearningPath(@PathVariable Long id) {
        return ResponseEntity.ok(learningPathService.delete(id));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> togglePublish(@PathVariable Long id) {
        return ResponseEntity.ok(learningPathService.togglePublish(id));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Object>> getPublicLearningPaths(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(learningPathService.getAll(pageable, level, search));
    }

    @GetMapping("/public/{slug}")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> getLearningPathBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(learningPathService.getBySlug(slug));
    }
}
