package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.application.dto.SignInRequest;
import org.rap.algotutorbe.iam.application.dto.SignUpRequest;
import org.rap.algotutorbe.iam.application.dto.TokenResponse;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.application.dto.UpdateProfileRequest;
import org.rap.algotutorbe.iam.application.dto.ChangePasswordRequest;
import org.rap.algotutorbe.iam.dto.SessionResponse;
import org.rap.algotutorbe.iam.application.mapper.UserMapper;
import org.rap.algotutorbe.iam.domain.model.RoleCode;
import org.rap.algotutorbe.iam.domain.model.User;

import java.util.List;
import java.util.UUID;
import org.rap.algotutorbe.iam.domain.repositories.RoleRepository;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.dto.UserProfileResponse;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.iam.infrastructure.jwt.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService extends BaseService {
    private final JwtUtil jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public TokenResponse processSignIn(SignInRequest payload, String ipAddress, String deviceInfo) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(payload.username(), payload.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return buildToken(userDetails, ipAddress, deviceInfo);
        } catch (AuthenticationException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

    }

    public void processSignUp(SignUpRequest request) {
        validate(request);
        userRepository.save(buildUserEntity(request));
        log.info("Created user with email {} successfully", request.email());
    }

    private User buildUserEntity(SignUpRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHashed(passwordEncoder.encode(request.password()));
        user.setRole(roleRepository.findByCode(RoleCode.USER)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR)));
        user.setEnabled(true);
        return user;
    }

    public TokenResponse processRefresh(String refreshToken, String ipAddress, String deviceInfo) {
        User user = refreshTokenService.verify(refreshToken);
        SecurityUser securityUser = new SecurityUser(user);
        log.info("Rotate refresh token success for user {}", user.getId());
        return rotateToken(refreshToken, securityUser, ipAddress, deviceInfo);
    }

    public void logout(String refreshToken) {
        refreshTokenService.invalidate(refreshToken);
        SecurityContextHolder.clearContext();
    }

    private TokenResponse rotateToken(String refreshToken, UserDetails userDetails, String ipAddress, String deviceInfo) {
        Instant now = Instant.now();
        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(userDetails, now))
                .refreshToken(refreshTokenService.rotate(refreshToken, ipAddress, deviceInfo))
                .build();
    }

    private TokenResponse buildToken(UserDetails userDetails, String ipAddress, String deviceInfo) {
        Instant now = Instant.now();
        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(userDetails, now))
                .refreshToken(refreshTokenService.createRefreshToken(userDetails, now, ipAddress, deviceInfo))
                .build();
    }


    private void validate(SignUpRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_INUSE);
        }
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new AppException(ErrorCode.USERNAME_TAKEN);
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }
    }

    public UserResponse getUserInfo() {
        User user = getCurrentUserOrThrow();
        UserResponse response = userMapper.toResponse(user);
        response.setRole(user.getRole().getName());
        return response;
    }

    public UserProfileResponse getUserProfile() {
        User user = getCurrentUserOrThrow();
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                null
        );
    }

    private User getCurrentUserOrThrow() {
        return userRepository.findById(getCurrentUserIdOrThrow())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    public List<SessionResponse> getActiveSessions(UUID userId, String currentTokenStr) {
        UUID currentUuid = null;
        if (currentTokenStr != null) {
            try {
                currentUuid = UUID.fromString(currentTokenStr);
            } catch (IllegalArgumentException ignored) {}
        }
        final UUID finalCurrentUuid = currentUuid;
        return refreshTokenService.getActiveSessions(userId).stream()
                .map(rt -> new SessionResponse(
                        rt.getId(),
                        rt.getIpv4Address(),
                        rt.getDeviceInfo(),
                        rt.getCreatedAt(),
                        rt.getExpiryDate(),
                        finalCurrentUuid != null && rt.getToken().equals(finalCurrentUuid)
                ))
                .toList();
    }

    public void terminateSession(UUID userId, Long sessionId) {
        refreshTokenService.terminateSession(userId, sessionId);
    }

    public void terminateOtherSessions(UUID userId, String currentTokenStr) {
        refreshTokenService.terminateOtherSessions(userId, currentTokenStr);
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getCurrentUserOrThrow();
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USERNAME_TAKEN);
        }
        if (!user.getEmail().equals(request.email())) {
            var existing = userRepository.findByEmail(request.email());
            if (existing.isPresent()) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_INUSE);
            }
        }
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setAvatar(request.avatar());
        userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getCurrentUserOrThrow();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHashed())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }
        user.setPasswordHashed(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
