package org.rap.algotutorbe.iam.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.api.PageResponse;
import org.rap.algotutorbe.iam.application.dto.AdminCreateUserRequest;
import org.rap.algotutorbe.iam.application.dto.BlockUserRequest;
import org.rap.algotutorbe.iam.application.dto.ChangeUserRoleRequest;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.application.services.UserService;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<UserResponse> users = userService.getAllUsers(search, pageable);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @RequestBody @Valid AdminCreateUserRequest payload) {
        UserResponse response = userService.createUserByAdmin(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.buildSuccess(response));
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<UserResponse>> blockUser(
            @PathVariable UUID id,
            @RequestBody @Valid BlockUserRequest payload,
            @AuthenticationPrincipal SecurityUser principal) {
        UserResponse response = userService.blockUser(id, payload, principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<ApiResponse<UserResponse>> unblockUser(
            @PathVariable UUID id) {
        UserResponse response = userService.unblockUser(id);
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable UUID id,
            @RequestBody @Valid ChangeUserRoleRequest payload,
            @AuthenticationPrincipal SecurityUser principal) {
        UserResponse response = userService.changeUserRole(id, payload, principal.getId());
        return ResponseEntity.ok(ApiResponse.buildSuccess(response));
    }
}
