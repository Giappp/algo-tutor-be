package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.TopicWithLessonsDTO;
import org.rap.algotutorbe.learning.models.Topic;

@Mapper(config = GlobalMapperConfig.class, uses = {LessonWithProgressMapper.class})
public interface TopicWithLessonsMapper {

    @Mapping(target = "lessonCount", expression = "java(entity.getLessons() != null ? entity.getLessons().size() : 0)")
    @Mapping(target = "lessons", source = "lessons")
    @Mapping(target = "unlocked", ignore = true) // Will be set in service layer
    @Mapping(target = "completed", ignore = true) // Will be set in service layer
    @Mapping(target = "completedLessons", ignore = true) // Will be set in service layer
    @Mapping(target = "totalLessons", expression = "java(entity.getLessons() != null ? entity.getLessons().size() : 0)")
    TopicWithLessonsDTO toDto(Topic entity);
}
