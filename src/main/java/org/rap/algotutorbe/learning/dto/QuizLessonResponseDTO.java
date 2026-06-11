package org.rap.algotutorbe.learning.dto;

import java.util.List;

public record QuizLessonResponseDTO(
        Long id,
        String title,
        Boolean isPublished,
        String type,
        Integer displayOrder,
        String difficulty,
        Integer passingScore,
        Integer timeLimitMinutes,
        List<QuizQuestionResponseDTO> questions
) {
}
