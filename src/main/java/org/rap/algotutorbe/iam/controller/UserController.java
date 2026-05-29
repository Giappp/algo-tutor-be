package org.rap.algotutorbe.iam.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.application.dto.UpdateProfileRequest;
import org.rap.algotutorbe.iam.application.dto.ChangePasswordRequest;
import org.rap.algotutorbe.iam.application.services.AuthService;
import org.rap.algotutorbe.iam.application.services.UserEnrollmentService;
import org.rap.algotutorbe.iam.application.services.UserService;
import org.rap.algotutorbe.iam.dto.CurrentLessonResponse;
import org.rap.algotutorbe.iam.dto.EnrollmentProgressResponse;
import org.rap.algotutorbe.iam.dto.UserProfileResponse;
import org.rap.algotutorbe.iam.dto.SessionResponse;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.repositories.QuizAttemptRepository;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserEnrollmentService userEnrollmentService;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;

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

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestBody @Valid UpdateProfileRequest payload) {
        authService.updateProfile(principal.getId(), payload);
        return ResponseEntity.ok(ApiResponse.buildMessage("Profile updated successfully"));
    }

    @PutMapping("/me/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestBody @Valid ChangePasswordRequest payload) {
        authService.changePassword(principal.getId(), payload);
        return ResponseEntity.ok(ApiResponse.buildMessage("Password changed successfully"));
    }

    @GetMapping("/me/current-lesson")
    public ResponseEntity<ApiResponse<CurrentLessonResponse>> getCurrentLesson(
            @AuthenticationPrincipal SecurityUser principal) {
        Optional<CurrentLessonResponse> result = userEnrollmentService.getCurrentLesson(principal.getId());

        return result.map(currentLessonResponse -> ResponseEntity.ok(ApiResponse.buildSuccess(currentLessonResponse)))
                .orElseGet(() -> ResponseEntity.noContent().build());
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
    public ResponseEntity<ApiResponse<Object>> getActivityHeatmap(
            @RequestParam(name = "year") int year,
            @AuthenticationPrincipal SecurityUser principal) {
        Instant startDate = Instant.parse(year + "-01-01T00:00:00Z");
        Instant endDate = Instant.parse((year + 1) + "-01-01T00:00:00Z");

        List<Instant> submissionDates = submissionRepository.findSubmissionDates(principal.getId(), startDate, endDate);
        List<Instant> quizDates = quizAttemptRepository.findQuizAttemptDates(principal.getId(), startDate, endDate);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
        Map<String, Long> heatmap = new HashMap<>();

        for (Instant date : submissionDates) {
            String day = formatter.format(date);
            heatmap.put(day, heatmap.getOrDefault(day, 0L) + 1);
        }

        for (Instant date : quizDates) {
            String day = formatter.format(date);
            heatmap.put(day, heatmap.getOrDefault(day, 0L) + 1);
        }

        return ResponseEntity.ok(ApiResponse.buildSuccess(new TreeMap<>(heatmap)));
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions(
            @AuthenticationPrincipal SecurityUser principal,
            @CookieValue(name = "refresh-token", required = false) String refreshToken) {
        List<SessionResponse> sessions = authService.getActiveSessions(principal.getId(), refreshToken);
        return ResponseEntity.ok(ApiResponse.buildSuccess(sessions));
    }

    @DeleteMapping("/me/sessions/{id}")
    public ResponseEntity<ApiResponse<String>> terminateSession(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable Long id) {
        authService.terminateSession(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.buildMessage("Session terminated successfully"));
    }

    @DeleteMapping("/me/sessions/other")
    public ResponseEntity<ApiResponse<String>> terminateOtherSessions(
            @AuthenticationPrincipal SecurityUser principal,
            @CookieValue(name = "refresh-token") String refreshToken) {
        authService.terminateOtherSessions(principal.getId(), refreshToken);
        return ResponseEntity.ok(ApiResponse.buildMessage("Other sessions terminated successfully"));
    }
}
