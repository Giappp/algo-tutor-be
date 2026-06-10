package org.rap.algotutorbe.iam.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.domain.repositories.RefreshTokenRepository;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, userRepository);
    }

    @Test
    void verify_shouldRejectNullTokenAsInvalidToken() {
        assertInvalidToken(null);
    }

    @Test
    void verify_shouldRejectBlankTokenAsInvalidToken() {
        assertInvalidToken("   ");
    }

    @Test
    void verify_shouldRejectMalformedTokenAsInvalidToken() {
        assertInvalidToken("not-a-uuid");
    }

    private void assertInvalidToken(String token) {
        assertThatThrownBy(() -> refreshTokenService.verify(token))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("error", ErrorCode.INVALID_TOKEN);

        verifyNoInteractions(refreshTokenRepository, userRepository);
    }
}
