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
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.security.jwt.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;


    public String createRefreshToken(UserDetails userDetails, Instant now, String ipAddress, String deviceInfo) {
        User user = getUserByUsername(userDetails.getUsername());
        return generateAndSaveTokenForUser(user, now, ipAddress, deviceInfo);
    }

    public User verify(String tokenStr) {
        RefreshToken validToken = getAndValidateToken(tokenStr);
        return validToken.getUser();
    }

    @Transactional
    public String rotate(String tokenStr, String ipAddress, String deviceInfo) {
        // 1. Lấy và kiểm tra token cũ
        RefreshToken oldToken = getAndValidateToken(tokenStr);
        User user = oldToken.getUser();

        // 2. Xóa token cũ
        deleteToken(oldToken);
        log.info("Rotated refresh token for user {}", user.getId());

        return generateAndSaveTokenForUser(user, Instant.now(), ipAddress, deviceInfo);
    }

    public void invalidate(String tokenStr) {
        RefreshToken token = fetchTokenFromString(tokenStr);
        deleteToken(token);
        log.info("Deleted refresh token for user {}", token.getUser().getId());
    }

    public List<RefreshToken> getActiveSessions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return refreshTokenRepository.findByUser(user).stream()
                .filter(rt -> !rt.isExpired())
                .toList();
    }

    @Transactional
    public void terminateSession(UUID userId, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
        if (!token.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        deleteToken(token);
        log.info("Terminated session {} for user {}", sessionId, userId);
    }

    @Transactional
    public void terminateOtherSessions(UUID userId, String currentTokenStr) {
        UUID currentUuid = parseUUID(currentTokenStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<RefreshToken> allSessions = refreshTokenRepository.findByUser(user);
        for (RefreshToken rt : allSessions) {
            if (!rt.getToken().equals(currentUuid)) {
                deleteToken(rt);
            }
        }
        log.info("Terminated all other sessions for user {}", userId);
    }

    // --- CÁC HÀM PRIVATE HELPER (Phục vụ cho nguyên lý SRP - Đơn nhiệm) ---

    /**
     * Chỉ chịu trách nhiệm: Lắp ráp thực thể RefreshToken (Không query DB)
     */
    private RefreshToken buildTokenEntity(User user, Instant now, String ipAddress, String deviceInfo) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(now.plusMillis(refreshTokenExpirationMs));
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setIpv4Address(ipAddress);
        refreshToken.setDeviceInfo(deviceInfo);
        return refreshToken;
    }


    private String generateAndSaveTokenForUser(User user, Instant now, String ipAddress, String deviceInfo) {
        RefreshToken newToken = buildTokenEntity(user, now, ipAddress, deviceInfo);
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
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

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
