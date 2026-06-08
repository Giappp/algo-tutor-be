package org.rap.algotutorbe.iam.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.application.dto.AdminCreateUserRequest;
import org.rap.algotutorbe.iam.application.dto.BlockUserRequest;
import org.rap.algotutorbe.iam.application.dto.ChangeUserRoleRequest;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.application.mapper.UserMapper;
import org.rap.algotutorbe.iam.domain.model.Role;
import org.rap.algotutorbe.iam.domain.model.User;
import org.rap.algotutorbe.iam.domain.repositories.RoleRepository;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.iam.dto.UserRoadMapResponse;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userRepository.findByUserNameWithRole(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return new SecurityUser(user);
    }

    public List<UserRoadMapResponse> getUserRoadmaps(UUID id) {
        User user = getUserOrThrow(id);
        if (user.getEnrollments() == null || user.getEnrollments().isEmpty()) {
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

    public PageResponse<UserResponse> getAllUsers(String search, Pageable pageable) {
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<User> usersPage = userRepository.searchUsers(searchTerm, pageable);
        Page<UserResponse> responsePage = usersPage.map(this::toResponseWithRole);
        return PageResponse.of(responsePage);
    }

    @Transactional
    public UserResponse createUserByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USERNAME_TAKEN);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_INUSE);
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        Role role = roleRepository.findByCode(request.role())
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHashed(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setEnabled(request.enabled() == null || request.enabled());

        User savedUser = userRepository.save(user);
        return toResponseWithRole(savedUser);
    }

    @Transactional
    public UserResponse blockUser(UUID id, BlockUserRequest request, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new AppException(ErrorCode.CONFLICT_RESOURCE);
        }

        User user = getUserOrThrow(id);
        user.setEnabled(false);
        user.setBlockReason(request.reason());

        User savedUser = userRepository.save(user);
        return toResponseWithRole(savedUser);
    }

    @Transactional
    public UserResponse unblockUser(UUID id) {
        User user = getUserOrThrow(id);
        user.setEnabled(true);
        user.setBlockReason(null);

        User savedUser = userRepository.save(user);
        return toResponseWithRole(savedUser);
    }

    @Transactional
    public UserResponse changeUserRole(UUID id, ChangeUserRoleRequest request, UUID currentUserId) {
        if (id.equals(currentUserId)) {
            throw new AppException(ErrorCode.CONFLICT_RESOURCE);
        }

        User user = getUserOrThrow(id);
        Role role = roleRepository.findByCode(request.role())
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));

        user.setRole(role);
        User savedUser = userRepository.save(user);
        return toResponseWithRole(savedUser);
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private UserResponse toResponseWithRole(User user) {
        UserResponse response = userMapper.toResponse(user);
        if (user.getRole() != null) {
            response.setRole(user.getRole().getName());
        }
        return response;
    }
}
