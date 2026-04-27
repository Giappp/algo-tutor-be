package org.rap.algotutorbe.iam.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.iam.application.dto.SignInRequest;
import org.rap.algotutorbe.iam.application.dto.SignUpRequest;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.application.services.AuthService;
import org.rap.algotutorbe.iam.application.services.CookieService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/iam")
@RequiredArgsConstructor
public class IAMController {
    private final AuthService authService;
    private final CookieService cookieService;

    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<String>> signIn(@RequestBody @Valid SignInRequest payload) {
        var token = authService.processSignIn(payload);

        var accessTokenCookie = cookieService.createAccessTokenCookie(token.accessToken());
        var refreshTokenCookie = cookieService.createRefreshTokenCookie(token.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.buildSuccess("Login success"));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signUp(@RequestBody @Valid SignUpRequest payload, HttpServletRequest request) {
        authService.processSignUp(payload);
        return ResponseEntity.created(URI.create(request.getRequestURI()))
                .body(ApiResponse.buildMessage("Sign up success"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(@CookieValue(name = "refresh-token", required = false) String refreshToken) {
        var token = authService.processRefresh(refreshToken);
        var accessTokenCookie = cookieService.createAccessTokenCookie(token.accessToken());
        var refreshTokenCookie = cookieService.createRefreshTokenCookie(token.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.buildMessage("Refresh success"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @CookieValue(name = "refresh-token") String refreshToken
    ) {
        authService.logout(refreshToken);
        var cleanAccessTokenCookie = cookieService.cleanAccessTokenCookie();
        var cleanRefreshTokenCookie = cookieService.cleanRefreshTokenCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanAccessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, cleanRefreshTokenCookie.toString())
                .body(ApiResponse.buildMessage("Logout success"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        var user = authService.getUserInfo();
        return ResponseEntity.ok(ApiResponse.buildSuccess(user));
    }

}
