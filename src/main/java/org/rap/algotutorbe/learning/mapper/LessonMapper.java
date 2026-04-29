package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;

@Mapper(config = GlobalMapperConfig.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LessonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    void updateFromRequest(LessonRequestDTO request, @MappingTarget Lesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    @Mapping(target = "editorials", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    @Mapping(target = "baseTimeLimitMs", ignore = true)
    @Mapping(target = "baseMemoryLimitMb", ignore = true)
    @Mapping(target = "starterCode", ignore = true)
    @Mapping(target = "constraints", ignore = true)
    @Mapping(target = "hints", ignore = true)
    @Mapping(target = "examples", ignore = true)
    @Mapping(target = "keyInsights", ignore = true)
    void updateFromRequest(CodingLessonRequestDTO request, @MappingTarget CodingLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "passingScore", ignore = true)
    @Mapping(target = "timeLimitMinutes", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    void updateFromRequest(QuizLessonRequestDTO request, @MappingTarget QuizLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    void updateFromRequest(TheoryLessonRequestDTO request, @MappingTarget TheoryLesson lesson);

    default LessonResponseDTO toResponse(Lesson lesson) {
        if (lesson == null) return null;
        return new LessonResponseDTO(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getSlug(),
                lesson.getType(),
                lesson.getContent(),
                lesson.getOrderIndex(),
                lesson.getIsPublished(),
                lesson.getDifficulty(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }

    default Lesson toEntity(LessonRequestDTO request) {
        if (request == null) return null;
        return switch (request.getType()) {
            case THEORY -> new TheoryLesson();
            case QUIZ -> new QuizLesson();
            case CODING -> new CodingLesson();
        };
    }
}
