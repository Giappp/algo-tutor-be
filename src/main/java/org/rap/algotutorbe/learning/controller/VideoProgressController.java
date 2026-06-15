package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.video.VideoProgressResponse;
import org.rap.algotutorbe.learning.dto.video.VideoProgressUpdateRequest;
import org.rap.algotutorbe.learning.services.VideoProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lessons/{slug}/video/progress")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VideoProgressController {
    private final VideoProgressService videoProgressService;

    @GetMapping
    public ResponseEntity<ApiResponse<VideoProgressResponse>> getProgress(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(videoProgressService.getProgress(slug)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<VideoProgressResponse>> updateProgress(
            @PathVariable String slug,
            @Valid @RequestBody VideoProgressUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(videoProgressService.updateProgress(slug, request)));
    }
}
