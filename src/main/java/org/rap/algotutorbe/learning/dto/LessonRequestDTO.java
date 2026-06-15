package org.rap.algotutorbe.learning.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.LessonType;

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
        @JsonSubTypes.Type(value = QuizLessonRequestDTO.class, name = "QUIZ"),
        @JsonSubTypes.Type(value = VideoLessonRequestDTO.class, name = "VIDEO")
})
public abstract class LessonRequestDTO {
    @NotNull
    @NotBlank
    private String title;
    @NotNull
    private LessonType type;
    @NotNull
    private Difficulty difficulty;
    private Integer displayOrder;
}
