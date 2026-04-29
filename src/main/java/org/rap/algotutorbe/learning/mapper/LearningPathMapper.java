package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.models.LearningPath;

@Mapper(config = GlobalMapperConfig.class)
public interface LearningPathMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "slug", ignore = true)
    LearningPath toEntity(LearningPathRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "slug", ignore = true)
    void updateEntity(@MappingTarget LearningPath entity, LearningPathRequestDTO request);
}
