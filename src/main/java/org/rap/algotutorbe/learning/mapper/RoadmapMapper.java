package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.RoadmapDetailResponseDTO;
import org.rap.algotutorbe.learning.dto.landing.RoadmapResponseDTO;
import org.rap.algotutorbe.learning.models.LearningPath;

@Mapper(config = GlobalMapperConfig.class,
        unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
        uses = {TopicWithLessonsMapper.class})
public interface RoadmapMapper {

    @Mapping(target = "lessonCount", expression = "java(computeLessonCount(entity))")
    @Mapping(target = "topicCount", expression = "java(computeTopicCount(entity))")
    @Mapping(target = "enrollmentCount", expression = "java(entity.getEnrollments() != null ? entity.getEnrollments().size() : 0)")
    RoadmapResponseDTO toResponse(LearningPath entity);

    @Mapping(target = "level", expression = "java(entity.getLevel() != null ? entity.getLevel().name() : null)")
    @Mapping(target = "enrollmentCount", expression = "java(entity.getEnrollments() != null ? entity.getEnrollments().size() : 0)")
    @Mapping(target = "topicCount", expression = "java(entity.getTopics() != null ? entity.getTopics().size() : 0)")
    @Mapping(target = "lessonCount", expression = "java(computeLessonCount(entity))")
    @Mapping(target = "topics", source = "topics")
    RoadmapDetailResponseDTO toDetailDto(LearningPath entity);

    default int computeLessonCount(LearningPath learningPath) {
        return LearningPathMapper.countPublishedLessons(learningPath);
    }

    default int computeTopicCount(LearningPath learningPath) {
        return TopicMapper.countPublishedTopic(learningPath);
    }
}
