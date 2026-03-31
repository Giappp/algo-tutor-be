package org.rap.algotutorbe.iam.application.services;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.application.dto.RefreshTokenRequest;
import org.rap.algotutorbe.iam.application.dto.SignInRequest;
import org.rap.algotutorbe.iam.application.dto.SignUpRequest;
import org.rap.algotutorbe.iam.application.dto.TokenResponse;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.iam.infrastructure.jwt.JwtProvider;
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
        throw new AppException(ErrorCode.INVALID_CREDENTIALS);
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

    public TokenResponse processRefresh(RefreshTokenRequest payload) {
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
            throw new AppException(ErrorCode.EMAIL_ALREADY_INUSE);
        }
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            throw new AppException(ErrorCode.USERNAME_TAKEN);
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }
    }
}
