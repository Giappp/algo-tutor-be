package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.dto.landing.PublicCodingLessonResponseDTO;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.models.VideoLesson;

@Mapper(config = GlobalMapperConfig.class,
        uses = {TestCaseMapper.class, QuizQuestionMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LessonMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "editorials", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    CodingLesson toEntity(CodingLessonRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    @Mapping(target = "questions", ignore = true)
    QuizLesson toEntity(QuizLessonRequestDTO request);

    TheoryLessonResponseDTO toTheoryResponse(TheoryLesson lesson);

    QuizLessonResponseDTO toQuizResponse(QuizLesson lesson);

    CodingLessonResponseDTO toCodingResponse(CodingLesson lesson);

    VideoLessonResponseDTO toVideoResponse(VideoLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "estimatedMinutes", ignore = true)
    void updateTheoryFromDTO(TheoryLessonRequestDTO request, @MappingTarget TheoryLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    @Mapping(target = "questions", ignore = true)
    void updateQuizFromDTO(QuizLessonRequestDTO request, @MappingTarget QuizLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "editorials", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    void updateCodingFromDTO(CodingLessonRequestDTO request, @MappingTarget CodingLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "sourceObjectKey", ignore = true)
    @Mapping(target = "thumbnailObjectKey", ignore = true)
    @Mapping(target = "durationSeconds", ignore = true)
    @Mapping(target = "fileSizeBytes", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "processingStatus", ignore = true)
    void updateVideoFromDTO(VideoLessonRequestDTO request, @MappingTarget VideoLesson lesson);


    default LessonResponseDTO toResponse(Lesson lesson) {
        if (lesson == null) return null;
        return new LessonResponseDTO(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getSlug(),
                lesson.getType(),
                lesson.getDisplayOrder(),
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
            case VideoLesson video -> toVideoResponse(video);
            default -> throw new IllegalStateException("Unknown lesson type: " + lesson.getClass().getSimpleName());
        };
    }

    default PublicCodingLessonResponseDTO toPublicCodingResponse(CodingLesson lesson) {
        if (lesson == null) return null;
        return new PublicCodingLessonResponseDTO(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getStatement(),
                lesson.getDisplayOrder(),
                lesson.getDifficulty() != null ? lesson.getDifficulty().name() : null,
                lesson.getBaseTimeLimitMs(),
                lesson.getBaseMemoryLimitMb(),
                lesson.getConstraints(),
                lesson.getStarterCode(),
                lesson.getHints(),
                lesson.getExamples(),
                null,
                null
        );
    }

    default Lesson toEntity(LessonRequestDTO request) {
        if (request == null) return null;
        return switch (request.getType()) {
            case THEORY -> new TheoryLesson();
            case QUIZ -> new QuizLesson();
            case CODING -> new CodingLesson();
            case VIDEO -> new VideoLesson();
        };
    }
}
