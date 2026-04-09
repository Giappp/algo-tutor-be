package org.rap.algotutorbe.common.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BaseService {
    protected Optional<SecurityUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof SecurityUser securityUser) {
            return Optional.of(securityUser);
        }

        return Optional.empty();

    }

    protected Long getCurrentUserIdOrThrow() {
        return getCurrentUser()
                .map(SecurityUser::getId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }

    protected String getCurrentUserNameOrThrow() {
        return getCurrentUser()
                .map(SecurityUser::getUsername)
                .orElseThrow(() -> new AppException(ErrorCode.ACCESS_DENIED));
    }
}
