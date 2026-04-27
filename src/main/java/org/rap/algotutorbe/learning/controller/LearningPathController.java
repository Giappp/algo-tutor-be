package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
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
    public ResponseEntity<ApiResponse<Object>> createLearningPath(@RequestBody @Valid LearningPathRequestDTO request) {
        return ResponseEntity.ok(learningPathService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> updateLearningPath(@PathVariable Long id, @RequestBody @Valid LearningPathRequestDTO request) {
        return ResponseEntity.ok(learningPathService.update(id, request));
    }

    @PostMapping("/{id}/topics")
    public ResponseEntity<ApiResponse<Object>> addTopicToLearningPath(@PathVariable Long id, @RequestBody @Valid TopicRequestDTO request) {
        return ResponseEntity.ok(learningPathService.addTopic(id, request));
    }
}
