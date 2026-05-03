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
        uses = {TestCaseMapper.class, QuizQuestionMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LessonMapper {

    TheoryLessonResponseDTO toTheoryResponse(TheoryLesson lesson);

    QuizLessonResponseDTO toQuizResponse(QuizLesson lesson);

    CodingLessonResponseDTO toCodingResponse(CodingLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true) // topic is set separately in the service layer
    @Mapping(target = "slug", ignore = true)
    // slug is generated from title in the service layer
    @Mapping(target = "isPublished", ignore = true)
        // isPublished is managed separately in the service layer
    void updateFromRequest(LessonRequestDTO request, @MappingTarget Lesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true) // topic is set separately in the service layer
    @Mapping(target = "slug", ignore = true)
    // slug is generated from title in the service layer
    @Mapping(target = "isPublished", ignore = true) // isPublished is managed separately in the service layer
    @Mapping(target = "editorials", ignore = true) // editorials are managed separately in the service layer
    @Mapping(target = "submissions", ignore = true)
        // submissions are managed separately in the service layer
    void updateFromRequest(CodingLessonRequestDTO request, @MappingTarget CodingLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true) // topic is set separately in the service layer
    @Mapping(target = "slug", ignore = true)  // slug is generated from title in the service layer
    @Mapping(target = "isPublished", ignore = true) // isPublished is managed separately in the service layer
    @Mapping(target = "attempts", ignore = true)
        // attempts are managed separately
    void updateFromRequest(QuizLessonRequestDTO request, @MappingTarget QuizLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "topic", ignore = true) // topic is set separately in the service layer
    @Mapping(target = "slug", ignore = true) // slug is generated from title in the service layer
    @Mapping(target = "isPublished", ignore = true)
        // isPublished is managed separately in the service layer
    void updateFromRequest(TheoryLessonRequestDTO request, @MappingTarget TheoryLesson lesson);

    default LessonResponseDTO toResponse(Lesson lesson) {
        if (lesson == null) return null;
        return new LessonResponseDTO(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getSlug(),
                lesson.getType(),
                lesson.getOrderIndex(),
                lesson.getIsPublished(),
                lesson.getDifficulty(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }

    default Object toDetailedResponse(Lesson lesson) {
        if (lesson == null) return null;
        return switch (lesson) {
            case TheoryLesson theory -> toTheoryResponse(theory);
            case QuizLesson quiz -> toQuizResponse(quiz);
            case CodingLesson coding -> toCodingResponse(coding);
            default -> throw new IllegalStateException("Unknown lesson type: " + lesson.getClass().getSimpleName());
        };
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
