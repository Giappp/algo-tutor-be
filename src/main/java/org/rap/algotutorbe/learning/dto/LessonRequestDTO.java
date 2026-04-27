package org.rap.algotutorbe.learning.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TheoryLessonRequestDTO.class, name = "THEORY"),
        @JsonSubTypes.Type(value = CodingLessonRequestDTO.class, name = "CODING"),
        @JsonSubTypes.Type(value = QuizLessonRequestDTO.class, name = "QUIZ")
})
public abstract class LessonRequestDTO {
    private String title;
    private String content;
    private int orderIndex;
    private String type;
}