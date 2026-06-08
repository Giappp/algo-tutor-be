package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CookieService {

    // --- BẮT ĐẦU PHẦN KHAI BÁO HẰNG SỐ (CONSTANTS) ---
    private static final String COOKIE_NAME_ACCESS_TOKEN = "access-token";
    private static final String COOKIE_NAME_REFRESH_TOKEN = "refresh-token";

    private static final String PATH_API_V1 = "/";

    private static final String SAME_SITE_STRICT = "Strict";
    // --- KẾT THÚC PHẦN KHAI BÁO HẰNG SỐ ---
    @Value(value = "${app.security.jwt.accessTokenExpirationMs}")
    private long accessTokenExpirationMs;
    @Value(value = "${app.security.jwt.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;
    @Value(value = "${app.security.securedCookie}")
    private boolean isSecured;

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return buildCookieToken(
                COOKIE_NAME_ACCESS_TOKEN,
                accessToken,
                PATH_API_V1,
                accessTokenExpirationMs,
                SAME_SITE_STRICT
        );
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return buildCookieToken(
                COOKIE_NAME_REFRESH_TOKEN,
                refreshToken,
                PATH_API_V1,
                refreshTokenExpirationMs,
                SAME_SITE_STRICT
        );
    }

    public ResponseCookie cleanAccessTokenCookie() {
        // Lỗi thiếu dấu '/' đã được khắc phục nhờ dùng chung hằng số PATH_API_V1
        return buildCleanCookie(COOKIE_NAME_ACCESS_TOKEN, PATH_API_V1);
    }

    public ResponseCookie cleanRefreshTokenCookie() {
        return buildCleanCookie(COOKIE_NAME_REFRESH_TOKEN, PATH_API_V1);
    }

    private ResponseCookie buildCookieToken(String cookieName, String value, String path, long maxAgeMs, String sameSite) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(isSecured)
                .path(path)
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(sameSite)
                .build();
    }

    private ResponseCookie buildCleanCookie(String name, String path) {
        return ResponseCookie.from(name)
                .httpOnly(true)
                .secure(isSecured)
                .path(path)
                .maxAge(0) // maxAge = 0 dùng để xóa cookie
                .build();
    }
}
