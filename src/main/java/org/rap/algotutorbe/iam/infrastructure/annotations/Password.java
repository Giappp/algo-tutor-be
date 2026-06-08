package org.rap.algotutorbe.iam.infrastructure.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import org.rap.algotutorbe.iam.infrastructure.config.Messages;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^(?=.*\\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[^\\w\\s:])(\\S){8,16}$")
@ReportAsSingleViolation
public @interface Password {

    String message() default Messages.Validation.PASSWORD;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

