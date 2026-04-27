package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.models.LearningPath;

@Mapper(config = GlobalMapperConfig.class)
public interface LearningPathMapper {
    @Mapping(target = "slug", expression = "java(slugGenerator.generateFrom(request.name()))")
    LearningPath toEntity(LearningPathRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", expression = "java(slugGenerator.generateFrom(request.name()))")
    void updateEntity(@MappingTarget LearningPath entity, LearningPathRequestDTO request);
}
