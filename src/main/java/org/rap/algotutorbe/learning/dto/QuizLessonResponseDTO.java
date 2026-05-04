package org.rap.algotutorbe.learning.dto;

import java.util.List;

public record QuizLessonResponseDTO(
        Long id,
        String title,
        String type,
        Integer orderIndex,
        Boolean isPublished,
        String difficulty,
        Integer passingScore,
        Integer timeLimitMinutes,
        List<QuizQuestionResponseDTO> questions
) {
}
