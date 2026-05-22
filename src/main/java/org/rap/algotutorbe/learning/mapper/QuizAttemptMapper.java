package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptResponse;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptSummary;
import org.rap.algotutorbe.learning.models.QuizAttempt;

@Mapper(config = GlobalMapperConfig.class)
public interface QuizAttemptMapper {
    @Mapping(target = "lessonProgressUpdated", expression = "java(quizAttempt.getPassed())")
    QuizAttemptResponse toResponse(QuizAttempt quizAttempt);

    QuizAttemptSummary toSummary(QuizAttempt quizAttempt);
}
