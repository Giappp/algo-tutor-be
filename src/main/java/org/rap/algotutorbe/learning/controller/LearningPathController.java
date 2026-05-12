package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.learning.dto.EnrollmentResponseDTO;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.dto.LearningPathResponseDTO;
import org.rap.algotutorbe.learning.enums.Level;
import org.rap.algotutorbe.learning.services.LearningPathService;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<LearningPathResponseDTO>> getAllLearningPaths(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(learningPathService.getAll(pageable, level, search));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> createLearningPath(@RequestBody @Valid LearningPathRequestDTO request) {
        return ResponseEntity.ok(learningPathService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> getLearningPathById(@PathVariable Long id) {
        return ResponseEntity.ok(learningPathService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> updateLearningPath(@PathVariable Long id, @RequestBody @Valid LearningPathRequestDTO request) {
        return ResponseEntity.ok(learningPathService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteLearningPath(@PathVariable Long id) {
        return ResponseEntity.ok(learningPathService.delete(id));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> togglePublish(@PathVariable Long id) {
        return ResponseEntity.ok(learningPathService.togglePublish(id));
    }

    @GetMapping("/public/{slug}")
    public ResponseEntity<ApiResponse<LearningPathResponseDTO>> getLearningPathBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(learningPathService.getBySlug(slug));
    }

    @PostMapping("/{slug}/enroll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EnrollmentResponseDTO>> enroll(@PathVariable String slug) {
        return ResponseEntity.ok(learningPathService.enroll(slug));
    }
}
