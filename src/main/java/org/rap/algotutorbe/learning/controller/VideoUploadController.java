package org.rap.algotutorbe.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.video.VideoUploadAbortRequest;
import org.rap.algotutorbe.learning.dto.video.VideoUploadCompleteRequest;
import org.rap.algotutorbe.learning.dto.video.VideoUploadCompleteResponse;
import org.rap.algotutorbe.learning.dto.video.VideoUploadInitiateRequest;
import org.rap.algotutorbe.learning.dto.video.VideoUploadInitiateResponse;
import org.rap.algotutorbe.learning.dto.video.VideoUploadPartResponse;
import org.rap.algotutorbe.learning.dto.video.VideoUploadPartsRequest;
import org.rap.algotutorbe.learning.services.VideoUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/lessons/{lessonId}/video/uploads")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class VideoUploadController {
    private final VideoUploadService videoUploadService;

    @PostMapping
    public ResponseEntity<ApiResponse<VideoUploadInitiateResponse>> initiate(
            @PathVariable Long lessonId,
            @Valid @RequestBody VideoUploadInitiateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(videoUploadService.initiate(lessonId, request)));
    }

    @PostMapping("/parts")
    public ResponseEntity<ApiResponse<List<VideoUploadPartResponse>>> createPartUrls(
            @PathVariable Long lessonId,
            @Valid @RequestBody VideoUploadPartsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(videoUploadService.createPartUrls(lessonId, request)));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<VideoUploadCompleteResponse>> complete(
            @PathVariable Long lessonId,
            @Valid @RequestBody VideoUploadCompleteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(videoUploadService.complete(lessonId, request)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> abort(
            @PathVariable Long lessonId,
            @Valid @RequestBody VideoUploadAbortRequest request
    ) {
        videoUploadService.abort(lessonId, request);
        return ResponseEntity.ok(ApiResponse.buildMessage("Video upload aborted"));
    }
}
