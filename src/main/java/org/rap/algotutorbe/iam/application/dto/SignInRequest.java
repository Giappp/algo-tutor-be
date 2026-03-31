package org.rap.algotutorbe.iam.application.dto;

import jakarta.validation.constraints.Email;
import lombok.NonNull;
import org.rap.algotutorbe.iam.infrastructure.config.Messages;
import org.rap.algotutorbe.iam.infrastructure.config.Password;

public record SignInRequest(@NonNull @Email(message = Messages.Validation.EMAIL) String email,
                            @NonNull @Password String password) {
}
