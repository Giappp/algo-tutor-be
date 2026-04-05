package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.application.dto.SignInRequest;
import org.rap.algotutorbe.iam.application.dto.SignUpRequest;
import org.rap.algotutorbe.iam.application.dto.TokenResponse;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
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
public class AuthService {
    private final UserService userService;
    private final JwtUtil jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public TokenResponse processSignIn(SignInRequest payload) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(payload.userName(), payload.password())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return buildToken(userDetails);
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
        user.setUserName(request.userName());
        user.setEmail(request.email());
        user.setPasswordHashed(passwordEncoder.encode(request.password()));
        return user;
    }

    public TokenResponse processRefresh(String refreshToken) {
        User user = refreshTokenService.verify(refreshToken);
        SecurityUser securityUser = new SecurityUser(user);
        log.info("Rotate refresh token success for user {}", user.getId());
        return rotateToken(refreshToken, securityUser);
    }

    public void logout(String refreshToken) {
        refreshTokenService.invalidate(refreshToken);
        SecurityContextHolder.clearContext();
    }

    private TokenResponse rotateToken(String refreshToken, UserDetails userDetails) {
        Instant now = Instant.now();
        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(userDetails, now))
                .refreshToken(refreshTokenService.rotate(refreshToken))
                .build();
    }

    private TokenResponse buildToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(userDetails, now))
                .refreshToken(refreshTokenService.createRefreshToken(userDetails, now))
                .build();
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
