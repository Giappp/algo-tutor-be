package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.dto.UserRoadMapResponse;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserNameWithRole(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return new SecurityUser(user);
    }

    public List<UserRoadMapResponse> getUserRoadmaps(UUID id) {
        User user = getUserOrThrow(id);
        if (user.getEnrollments().isEmpty()) {
            return List.of();
        }
        return user.getEnrollments().stream()
                .map(enrollment -> new UserRoadMapResponse(
                        enrollment.getLearningPath().getName(),
                        enrollment.getLearningPath().getSlug(),
                        enrollment.getProgressPercentage().intValue(),
                        null,
                        null))
                .toList();
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
