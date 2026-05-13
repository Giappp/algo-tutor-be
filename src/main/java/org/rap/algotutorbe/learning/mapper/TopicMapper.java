package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.TopicRequestDTO;
import org.rap.algotutorbe.learning.dto.TopicResponseDTO;
import org.rap.algotutorbe.learning.models.Topic;

@Mapper(config = GlobalMapperConfig.class, uses = {LessonMapper.class})
public interface TopicMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "learningPath", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    Topic toEntity(TopicRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "learningPath", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    void updateEntity(@MappingTarget Topic entity, TopicRequestDTO request);

    @Mapping(target = "learningPathId", expression = "java(entity.getLearningPath() != null ? entity.getLearningPath().getId() : null)")
    @Mapping(target = "lessonCount", expression = "java(entity.getLessons() != null ? entity.getLessons().size() : 0)")
    @Mapping(target = "lessons", source = "lessons")
    TopicResponseDTO toResponse(Topic entity);
}
