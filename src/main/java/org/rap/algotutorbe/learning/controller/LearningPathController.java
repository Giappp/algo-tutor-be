package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.CreateOrUpdateLearningPathRequest;
import org.rap.algotutorbe.learning.services.LearningPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/learning-paths")
@RequiredArgsConstructor
public class LearningPathController {
    private final LearningPathService learningPathService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> createLearningPath(@RequestBody @Valid CreateOrUpdateLearningPathRequest request) {
        return ResponseEntity.ok(learningPathService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> updateLearningPath(@PathVariable Long id, @RequestBody @Valid CreateOrUpdateLearningPathRequest request) {
        return ResponseEntity.ok(learningPathService.update(id, request));
    }

    @PutMapping("/{id}/publish")
}
