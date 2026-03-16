package org.rap.algotutorbe.iam.internal.application.services;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.rap.algotutorbe.iam.SecurityUser;
import org.rap.algotutorbe.iam.internal.application.errors.ErrorCode;
import org.rap.algotutorbe.iam.internal.domain.exception.AuthException;
import org.rap.algotutorbe.iam.internal.domain.model.RefreshToken;
import org.rap.algotutorbe.iam.internal.domain.model.User;
import org.rap.algotutorbe.iam.internal.domain.repositories.RefreshTokenRepository;
import org.rap.algotutorbe.iam.internal.domain.repositories.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    private final UserRepository userRepository;

    private final long refreshTokenExpirationSecond = 7 * 24 * 60 * 60;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public String createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findById(userId).get());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationSecond));
        refreshToken.setToken(UUID.randomUUID());
        refreshTokenRepository.save(refreshToken);

        log.info("Create new session for user {}", userId);
        return refreshToken.getToken().toString();
    }

    public User verify(String token) {
        RefreshToken refreshToken = getRefreshToken(token);
        return refreshToken.getUser();
    }

    public String rotate(String token) {
        RefreshToken refreshToken = getRefreshToken(token);
        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);
        log.info("Rotate refresh token for user {}", user.getId());
        return createRefreshToken(user.getId());
    }

    public void invalidate(String token) {
        RefreshToken refreshToken = getRefreshToken(token);
        checkUser(refreshToken.getUser());
        log.info("Delete refresh token of user {}", refreshToken.getUser());
        refreshTokenRepository.delete(refreshToken);
    }

    private void checkUser(User user) {
        SecurityUser userDetails = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.user().getId();
        if (!user.getId().equals(userId)) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }
    }

    private @NonNull RefreshToken getRefreshToken(String token) {
        UUID uuidToken = UUID.fromString(token);
        RefreshToken refreshToken = refreshTokenRepository.findByToken(uuidToken)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_TOKEN));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new AuthException(ErrorCode.TOKEN_EXPIRED);
        }
        return refreshToken;
    }


    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUser(userRepository.findById(userId).get());
    }
}
