package org.rap.algotutorbe.learning.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class QuizLessonRequestDTO extends LessonRequestDTO {
    private Integer passingScore;
    private Integer timeLimitMinutes;
}