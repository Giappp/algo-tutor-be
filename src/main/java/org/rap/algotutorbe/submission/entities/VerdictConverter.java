package org.rap.algotutorbe.submission.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class VerdictConverter implements AttributeConverter<Verdict, String> {
    @Override
    public String convertToDatabaseColumn(Verdict attribute) {
        return attribute == null ? null : attribute.toDbValue();
    }

    @Override
    public Verdict convertToEntityAttribute(String dbData) {
        return Verdict.fromApiValue(dbData);
    }
}

