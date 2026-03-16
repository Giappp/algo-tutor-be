package org.rap.algotutorbe.iam.internal.web.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.iam.internal.application.services.AuthService;
import org.rap.algotutorbe.iam.internal.web.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/iam")
@RequiredArgsConstructor
public class IAMController {
    private final AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody @Valid SignInRequest payload, HttpServletRequest request) {
        var token = authService.processSignIn(payload, request);
        return ResponseEntity.ok(ApiResponse
                .<TokenResponse>builder()
                .data(token)
                .success(true)
                .build());
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody @Valid SignUpRequest payload, HttpServletRequest request) {
        authService.processSignUp(payload);
        return ResponseEntity.created(URI.create(request.getRequestURI()))
                .body(ApiResponse
                        .builder()
                        .messages("Sign Up Success")
                        .success(true)
                        .build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody @Valid RefreshTokenRequest payload, HttpServletRequest request) {
        var result = authService.processRefresh(payload, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .data(result)
                .success(true)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest.refreshToken());
        return ResponseEntity.ok(ApiResponse.builder()
                .messages("Logout success")
                .success(true)
                .build());
    }
}
