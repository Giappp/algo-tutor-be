package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.NonNull;
import org.rap.algotutorbe.iam.infrastructure.annotations.Password;

@Builder
public record SignUpRequest(@NonNull String username, @NonNull @Email String email,
                            @Password String password, @Password String confirmPassword) {
}
