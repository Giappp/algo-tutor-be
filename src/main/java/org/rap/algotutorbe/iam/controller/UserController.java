package org.rap.algotutorbe.iam.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.application.services.AuthService;
import org.rap.algotutorbe.iam.application.services.UserEnrollmentService;
import org.rap.algotutorbe.iam.application.services.UserService;
import org.rap.algotutorbe.iam.dto.CurrentLessonResponse;
import org.rap.algotutorbe.iam.dto.EnrollmentProgressResponse;
import org.rap.algotutorbe.iam.dto.UserProfileResponse;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;
    private final UserEnrollmentService userEnrollmentService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile() {
        UserProfileResponse profile = authService.getUserProfile();
        return ResponseEntity.ok(ApiResponse.buildSuccess(profile));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = authService.getUserInfo();
        return ResponseEntity.ok(ApiResponse.buildSuccess(user));
    }

    @GetMapping("/me/current-lesson")
    public ResponseEntity<ApiResponse<CurrentLessonResponse>> getCurrentLesson(
            @AuthenticationPrincipal SecurityUser principal) {
        Optional<CurrentLessonResponse> result = userEnrollmentService.getCurrentLesson(principal.getId());

        return result.map(currentLessonResponse ->
                ResponseEntity.ok(ApiResponse.buildSuccess(currentLessonResponse))).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/me/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentProgressResponse>>> getUserEnrollments(
            @AuthenticationPrincipal SecurityUser principal) {
        List<EnrollmentProgressResponse> enrollments = userEnrollmentService.getEnrollmentsSorted(principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(enrollments));
    }

    @GetMapping("/me/my-roadmaps")
    public ResponseEntity<ApiResponse<List<RoadmapResponseDTO>>> getUserRoadmaps(
            @AuthenticationPrincipal SecurityUser principal) {
        var response = userEnrollmentService.getUserRoadmaps(principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @GetMapping("/me/activity-heatmap")
    public ResponseEntity<ApiResponse<Object>> getActivityHeatmap(@RequestParam(name = "year") int year,
                                                                  @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.buildSuccess(""));
    }
}
