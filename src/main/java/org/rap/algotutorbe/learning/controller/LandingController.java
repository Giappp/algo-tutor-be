package org.rap.algotutorbe.learning.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.services.LandingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/landing")
@RequiredArgsConstructor
public class LandingController {
    private final LandingService landingService;

    @GetMapping("/roadmaps")
    public ResponseEntity<ApiResponse<List<RoadmapResponseDTO>>> getLearningPath() {
        List<RoadmapResponseDTO> roadmaps = landingService.getPublishedRoadmaps();
        return ResponseEntity.ok(ApiResponse.buildSuccess(roadmaps));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getPlatformStats() {
        return ResponseEntity.ok(ApiResponse.buildMessage("Landing page is under construction"));
    }

    @GetMapping("/testimonials")
    public ResponseEntity<ApiResponse<Object>> getTestimonials() {
        return ResponseEntity.ok(ApiResponse.buildMessage("Landing page is under construction"));
    }

    @GetMapping("/features")
    public ResponseEntity<ApiResponse<Object>> getFeatures() {
        return ResponseEntity.ok(ApiResponse.buildMessage("Landing page is under construction"));
    }

    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<Object>> getFAQs() {
        return ResponseEntity.ok(ApiResponse.buildMessage("Landing page is under construction"));
    }
}
