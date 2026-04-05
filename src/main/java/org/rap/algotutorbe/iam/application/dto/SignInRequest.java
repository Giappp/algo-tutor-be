package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import org.rap.algotutorbe.iam.infrastructure.annotations.Password;

public record SignInRequest(@NonNull @NotBlank String userName,
                            @NonNull @Password String password) {
}
