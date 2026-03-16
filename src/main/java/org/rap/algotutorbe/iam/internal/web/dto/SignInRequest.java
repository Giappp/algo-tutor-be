package org.rap.algotutorbe.iam.internal.web.dto;

import jakarta.validation.constraints.Email;
import lombok.NonNull;
import org.rap.algotutorbe.iam.internal.infrastructure.config.Messages;
import org.rap.algotutorbe.iam.internal.infrastructure.config.Password;

public record SignInRequest(@NonNull @Email(message = Messages.Validation.EMAIL) String email,
                            @NonNull @Password String password) {
}
