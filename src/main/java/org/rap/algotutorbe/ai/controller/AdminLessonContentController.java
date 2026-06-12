package org.rap.algotutorbe.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.ai.dto.AdminLessonContentGenerateRequest;
import org.rap.algotutorbe.ai.dto.AdminLessonContentGenerateResponse;
import org.rap.algotutorbe.ai.services.AdminLessonContentGenerationService;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai/lessons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLessonContentController {

    private final AdminLessonContentGenerationService generationService;

    @PostMapping("/{lessonId}/generate-content")
    public ResponseEntity<ApiResponse<AdminLessonContentGenerateResponse>> generateContent(
            @PathVariable Long lessonId,
            @Valid @RequestBody AdminLessonContentGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(generationService.generate(lessonId, request)));
    }
}
