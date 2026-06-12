package org.rap.algotutorbe.learning.dto;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuizLessonRequestDTO extends LessonRequestDTO {
    private Integer passingScore;
    private Integer timeLimitMinutes;
    @Valid
    private List<QuizQuestionDTO> questions;
}
