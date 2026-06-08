package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.iam.domain.model.RoleCode;
import org.rap.algotutorbe.iam.infrastructure.annotations.Password;

public record AdminCreateUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @Password String password,
        @Password String confirmPassword,
        @NotNull RoleCode role,
        Boolean enabled
) {}
