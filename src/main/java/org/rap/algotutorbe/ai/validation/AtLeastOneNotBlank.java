package org.rap.algotutorbe.ai.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that at least one of the specified fields is non-null and non-blank.
 */
@Documented
@Constraint(validatedBy = AtLeastOneNotBlankValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneNotBlank {

    String message() default "At least one of message or code must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String[] fields();
}
