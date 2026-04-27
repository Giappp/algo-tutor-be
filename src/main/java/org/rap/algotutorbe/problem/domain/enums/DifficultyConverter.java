package org.rap.algotutorbe.problem.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DifficultyConverter implements AttributeConverter<Difficulty, String> {
    @Override
    public String convertToDatabaseColumn(Difficulty attribute) {
        return attribute == null ? null : attribute.toApiValue();
    }

    @Override
    public Difficulty convertToEntityAttribute(String dbData) {
        return Difficulty.fromApiValue(dbData);
    }
}

