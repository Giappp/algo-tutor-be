package org.rap.algotutorbe.learning.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.learning.dto.LearningPathResponse;
import org.rap.algotutorbe.learning.dto.LearningPathSummaryResponse;
import org.rap.algotutorbe.learning.models.Level;
import org.rap.algotutorbe.learning.services.LearningPathService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/learning-paths")
@RequiredArgsConstructor
public class OnBoardingController {

    private final LearningPathService learningPathService;

    @GetMapping
    public ResponseEntity<PageResponse<LearningPathSummaryResponse>> list(
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(learningPathService.list(level, search, pageable));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(learningPathService.getBySlug(slug)));
    }
}
