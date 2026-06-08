package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.NotNull;
import org.rap.algotutorbe.iam.domain.model.RoleCode;

public record ChangeUserRoleRequest(
        @NotNull RoleCode role
) {}
