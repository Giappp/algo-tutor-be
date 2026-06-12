package org.rap.algotutorbe.learning.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TheoryLessonRequestDTO extends LessonRequestDTO {
    private String content;
    private Integer estimatedMinutes;
}
