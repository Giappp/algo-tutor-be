package org.rap.algotutorbe.learning.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuizLessonRequestDTO extends LessonRequestDTO {
    private List<QuizQuestionDTO> questions;
    private Integer passingScore;
    private Integer timeLimitMinutes;
}