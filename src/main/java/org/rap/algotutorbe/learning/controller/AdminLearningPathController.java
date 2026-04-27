package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.learning.dto.LearningPathSummaryResponse;
import org.rap.algotutorbe.learning.dto.request.CreateLearningPathRequest;
import org.rap.algotutorbe.learning.dto.request.UpdateLearningPathRequest;
import org.rap.algotutorbe.learning.services.LearningPathService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/learning-paths")
@RequiredArgsConstructor
@Slf4j
public class AdminLearningPathController {

    private final LearningPathService learningPathService;

    @GetMapping
    public ResponseEntity<PageResponse<LearningPathSummaryResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(learningPathService.list(null, null, pageable));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LearningPathSummaryResponse>> create(
            @RequestBody @Valid CreateLearningPathRequest request) {
        log.info("Creating learning path: {}", request.name());
        return ResponseEntity.ok(ApiResponse.buildSuccess(learningPathService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LearningPathSummaryResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateLearningPathRequest request) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(learningPathService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        learningPathService.delete(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess("Learning path deleted successfully"));
    }
}
