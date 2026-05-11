package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.models.LearningPath;

@Mapper(config = GlobalMapperConfig.class)
public interface RoadmapMapper {

    @Mapping(target = "topicCount", expression = "java(entity.getTopics() != null ? entity.getTopics().size() : 0)")
    @Mapping(target = "lessonCount", expression = "java(countPublishedLessons(entity))")
    RoadmapResponseDTO toResponse(LearningPath entity);

    default int countPublishedLessons(LearningPath learningPath) {
        if (learningPath.getTopics() == null) {
            return 0;
        }
        return learningPath.getTopics().stream()
                .flatMap(topic -> topic.getLessons().stream())
                .filter(lesson -> Boolean.TRUE.equals(lesson.getIsPublished()))
                .mapToInt(lesson -> 1)
                .sum();
    }
}
