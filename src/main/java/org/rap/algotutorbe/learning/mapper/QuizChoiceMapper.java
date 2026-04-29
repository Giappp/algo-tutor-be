package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.QuizChoiceRequestDTO;
import org.rap.algotutorbe.learning.dto.QuizChoiceResponseDTO;
import org.rap.algotutorbe.learning.models.QuizChoice;

@Mapper(config = GlobalMapperConfig.class)
public interface QuizChoiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "question", ignore = true)
    QuizChoice toEntity(QuizChoiceRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "question", ignore = true)
    void updateEntity(@MappingTarget QuizChoice entity, QuizChoiceRequestDTO dto);

    QuizChoiceResponseDTO toResponse(QuizChoice entity);
}
