package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.LearningPathRequestDTO;
import org.rap.algotutorbe.learning.dto.LearningPathResponseDTO;
import org.rap.algotutorbe.learning.models.LearningPath;

@Mapper(config = GlobalMapperConfig.class, uses = {TopicMapper.class, LessonMapper.class})
public interface LearningPathMapper {

    @Named("countTotalLessons")
    static int countTotalLessons(LearningPath lp) {
        if (lp.getTopics() == null) return 0;
        return lp.getTopics().stream()
                .mapToInt(t -> t.getLessons() != null ? t.getLessons().size() : 0)
                .sum();
    }

    @Named("countPublishedLessons")
    static int countPublishedLessons(LearningPath lp) {
        if (lp.getTopics() == null) return 0;
        return lp.getTopics().stream()
                .flatMap(t -> t.getLessons() != null ? t.getLessons().stream() : java.util.stream.Stream.empty())
                .mapToInt(l -> 1)
                .sum();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    LearningPath toEntity(LearningPathRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    void updateEntity(@MappingTarget LearningPath entity, LearningPathRequestDTO request);

    @Mapping(target = "topicCount", expression = "java(entity.getTopics() != null ? entity.getTopics().size() : 0)")
    @Mapping(target = "totalLessonCount", source = ".", qualifiedByName = "countTotalLessons")
    @Mapping(target = "publishedLessonCount", source = ".", qualifiedByName = "countPublishedLessons")
    @Mapping(target = "enrollmentCount", expression = "java(entity.getEnrollments() != null ? entity.getEnrollments().size() : 0)")
    @Mapping(target = "isPublished", source = "isPublished")
    @Mapping(target = "topics", source = "topics")
    LearningPathResponseDTO toResponse(LearningPath entity);
}