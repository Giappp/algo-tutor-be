package org.rap.algotutorbe.ai.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.RecordComponent;

/**
 * Validator that checks at least one of the specified fields in a record is non-null and non-blank.
 */
public class AtLeastOneNotBlankValidator implements ConstraintValidator<AtLeastOneNotBlank, Object> {

    private String[] fields;

    @Override
    public void initialize(AtLeastOneNotBlank constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        for (String fieldName : fields) {
            try {
                RecordComponent[] components = value.getClass().getRecordComponents();
                for (RecordComponent component : components) {
                    if (component.getName().equals(fieldName)) {
                        Object fieldValue = component.getAccessor().invoke(value);
                        if (fieldValue instanceof String str && !str.isBlank()) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }
}
