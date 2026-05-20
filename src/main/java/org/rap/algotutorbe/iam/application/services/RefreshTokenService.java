package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.domain.model.RefreshToken;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.RefreshTokenRepository;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.security.jwt.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;


    public String createRefreshToken(UserDetails userDetails, Instant now) {
        User user = getUserByUsername(userDetails.getUsername());
        return generateAndSaveTokenForUser(user, now);
    }

    public User verify(String tokenStr) {
        RefreshToken validToken = getAndValidateToken(tokenStr);
        return validToken.getUser();
    }

    @Transactional
    public String rotate(String tokenStr) {
        // 1. Lấy và kiểm tra token cũ
        RefreshToken oldToken = getAndValidateToken(tokenStr);
        User user = oldToken.getUser();

        // 2. Xóa token cũ
        deleteToken(oldToken);
        log.info("Rotated refresh token for user {}", user.getId());

        return generateAndSaveTokenForUser(user, Instant.now());
    }

    public void invalidate(String tokenStr) {
        RefreshToken token = fetchTokenFromString(tokenStr);
        deleteToken(token);
        log.info("Deleted refresh token for user {}", token.getUser().getId());
    }

    // --- CÁC HÀM PRIVATE HELPER (Phục vụ cho nguyên lý SRP - Đơn nhiệm) ---

    /**
     * Chỉ chịu trách nhiệm: Lắp ráp thực thể RefreshToken (Không query DB)
     */
    private RefreshToken buildTokenEntity(User user, Instant now) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(now.plusMillis(refreshTokenExpirationMs));
        refreshToken.setToken(UUID.randomUUID());
        return refreshToken;
    }


    private String generateAndSaveTokenForUser(User user, Instant now) {
        RefreshToken newToken = buildTokenEntity(user, now);
        refreshTokenRepository.save(newToken);
        return newToken.getToken().toString();
    }

    /**
     * Chỉ chịu trách nhiệm: Lấy token từ DB và kiểm tra tính hợp lệ
     */
    private RefreshToken getAndValidateToken(String tokenStr) {
        RefreshToken refreshToken = fetchTokenFromString(tokenStr);

        if (refreshToken.isExpired()) {
            deleteToken(refreshToken);
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }
        return refreshToken;
    }

    /**
     * Chỉ chịu trách nhiệm: Chuyển đổi String sang UUID và tìm kiếm trong DB
     */
    private RefreshToken fetchTokenFromString(String tokenStr) {
        UUID uuidToken = parseUUID(tokenStr);
        return refreshTokenRepository.findByToken(uuidToken)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
    }

    /**
     * Chỉ chịu trách nhiệm: Bắt lỗi an toàn khi parse chuỗi sang UUID
     */
    private UUID parseUUID(String tokenStr) {
        try {
            return UUID.fromString(tokenStr);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * Chỉ chịu trách nhiệm: Query User từ DB
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Chỉ chịu trách nhiệm: Thực thi thao tác xóa Token
     */
    private void deleteToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }
}