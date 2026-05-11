package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.QuizQuestionDTO;
import org.rap.algotutorbe.learning.dto.QuizQuestionResponseDTO;
import org.rap.algotutorbe.learning.models.QuizQuestion;

import java.util.List;

@Mapper(config = GlobalMapperConfig.class)
public interface QuizQuestionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    QuizQuestion toEntity(QuizQuestionDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    void updateEntity(@MappingTarget QuizQuestion entity, QuizQuestionDTO request);

    QuizQuestionResponseDTO toResponse(QuizQuestion entity);

    List<QuizQuestionResponseDTO> toResponseList(List<QuizQuestion> entities);
}
