package org.rap.algotutorbe.iam.internal.application.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.iam.SecurityUser;
import org.rap.algotutorbe.iam.internal.application.errors.ErrorCode;
import org.rap.algotutorbe.iam.internal.domain.exception.AuthException;
import org.rap.algotutorbe.iam.internal.domain.model.User;
import org.rap.algotutorbe.iam.internal.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.internal.infrastructure.jwt.JwtProvider;
import org.rap.algotutorbe.iam.internal.web.dto.RefreshTokenRequest;
import org.rap.algotutorbe.iam.internal.web.dto.SignInRequest;
import org.rap.algotutorbe.iam.internal.web.dto.SignUpRequest;
import org.rap.algotutorbe.iam.internal.web.dto.TokenResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenRepository;

    public TokenResponse processSignIn(SignInRequest payload, HttpServletRequest request) {
        var securityUser = userService.loadUserByUsername(payload.email());
        log.info("Sign in request attempt for account ${} with Ip: ${}", securityUser.getUsername(), request.getRemoteAddr());
        if (isMatchesPassword(payload, securityUser)) {
            log.info("User {} successfully logged in}", securityUser.user().getId());
            return buildToken(securityUser.user());
        }
        log.warn("Logged in attempted fail for user {}", securityUser.user().getId());
        throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
    }

    public void processSignUp(SignUpRequest request) {
        validate(request);
        userRepository.save(buildUserEntity(request));
        log.info("Created user with email {} successfully", request.email());
    }

    private User buildUserEntity(SignUpRequest request) {
        User user = new User();
        user.setUserName(request.userName());
        user.setEmail(request.email());
        user.setPasswordHashed(passwordEncoder.encode(request.password()));
        return user;
    }

    public TokenResponse processRefresh(RefreshTokenRequest payload, HttpServletRequest request) {
        String refreshToken = payload.refreshToken();
        User user = refreshTokenRepository.verify(refreshToken);
        log.info("Rotate refresh token success for user {}", user.getId());
        return rotateToken(refreshToken, user);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.invalidate(refreshToken);
        SecurityContextHolder.clearContext();
    }

    private TokenResponse rotateToken(String refreshToken, User user) {
        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(user))
                .refreshToken(refreshTokenRepository.rotate(refreshToken))
                .build();
    }

    private TokenResponse buildToken(User user) {
        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(user))
                .refreshToken(refreshTokenRepository.createRefreshToken(user.getId()))
                .build();
    }

    private boolean isMatchesPassword(SignInRequest request, SecurityUser user) {
        return passwordEncoder.matches(request.password(), user.getPassword());
    }

    private void validate(SignUpRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_INUSE);
        }
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new AuthException(ErrorCode.USERNAME_TAKEN);
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new AuthException(ErrorCode.PASSWORD_MISMATCH);
        }
    }
}
