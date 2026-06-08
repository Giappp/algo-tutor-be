package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
import org.rap.algotutorbe.learning.dto.TopicResponseDTO;
import org.rap.algotutorbe.learning.services.TopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {
    private final TopicService topicService;

    @PostMapping("/learning-paths/{pathId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopicResponseDTO>> createTopic(
            @PathVariable Long pathId,
            @RequestBody @Valid TopicRequestDTO request) {
        return ResponseEntity.ok(topicService.create(pathId, request));
    }

    @GetMapping("/learning-paths/{pathId}")
    public ResponseEntity<ApiResponse<List<TopicResponseDTO>>> getTopicsByLearningPath(@PathVariable Long pathId) {
        return ResponseEntity.ok(topicService.getByLearningPathId(pathId));
    }

    @GetMapping("/{topicId}")
    public ResponseEntity<ApiResponse<TopicResponseDTO>> getTopicById(@PathVariable Long topicId) {
        return ResponseEntity.ok(topicService.getById(topicId));
    }

    @PutMapping("/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TopicResponseDTO>> updateTopic(
            @PathVariable Long topicId,
            @RequestBody @Valid TopicRequestDTO request) {
        return ResponseEntity.ok(topicService.update(topicId, request));
    }

    @DeleteMapping("/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteTopic(@PathVariable Long topicId) {
        return ResponseEntity.ok(topicService.delete(topicId));
    }
}
