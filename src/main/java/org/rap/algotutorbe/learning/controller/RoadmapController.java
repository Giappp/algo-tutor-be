package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.services.RoadmapService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roadmaps")
@RequiredArgsConstructor
public class RoadmapController {
    private final RoadmapService roadmapService;

    @GetMapping
    public ResponseEntity<PageResponse<RoadmapResponseDTO>> getPublishedRoadmaps(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String level) {
        return ResponseEntity.ok(roadmapService.getPublishedRoadmaps(pageable, level));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<RoadmapDetailResponseDTO>> getRoadmapBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(roadmapService.getRoadmapBySlug(slug));
    }

    @PostMapping("/{slug}/enroll")
    public ResponseEntity<ApiResponse<EnrollmentResponseDTO>> enroll(@PathVariable String slug) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roadmapService.enroll(slug));
    }

    @PatchMapping("/{slug}/lessons/{lessonSlug}/progress")
    public ResponseEntity<ApiResponse<LessonProgressUpdateResponse>> updateLessonProgress(
            @PathVariable String slug,
            @PathVariable String lessonSlug,
            @RequestBody @Valid LessonProgressUpdateRequest request) {
        return ResponseEntity.ok(roadmapService.updateLessonProgress(slug, lessonSlug, request.status()));
    }

    @GetMapping("/{slug}/enrollment")
    public ResponseEntity<ApiResponse<EnrollmentDetailResponseDTO>> getEnrollment(@PathVariable String slug) {
        return ResponseEntity.ok(roadmapService.getEnrollment(slug));
    }
}
