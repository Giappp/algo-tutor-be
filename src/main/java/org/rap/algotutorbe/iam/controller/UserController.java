package org.rap.algotutorbe.iam.controller;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.application.services.AuthService;
import org.rap.algotutorbe.iam.dto.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

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
}
