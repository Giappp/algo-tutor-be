package org.rap.algotutorbe.learning.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.dto.landing.PublicCodingLessonResponseDTO;
import org.rap.algotutorbe.learning.dto.landing.PublicTestCaseResponseDTO;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;

import java.util.List;

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
    CodingLesson toEntity(CodingLessonRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    QuizLesson toEntity(QuizLessonRequestDTO request);

    TheoryLessonResponseDTO toTheoryResponse(TheoryLesson lesson);

    QuizLessonResponseDTO toQuizResponse(QuizLesson lesson);

    CodingLessonResponseDTO toCodingResponse(CodingLesson lesson);

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
    void updateQuizFromDTO(QuizLessonRequestDTO request, @MappingTarget QuizLesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "editorials", ignore = true)
    @Mapping(target = "submissions", ignore = true)
    void updateCodingFromDTO(CodingLessonRequestDTO request, @MappingTarget CodingLesson lesson);


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
            default -> throw new IllegalStateException("Unknown lesson type: " + lesson.getClass().getSimpleName());
        };
    }

    default PublicCodingLessonResponseDTO toPublicCodingResponse(CodingLesson lesson) {
        if (lesson == null) return null;
        List<PublicTestCaseResponseDTO> publicTestCases = lesson.getTestCases() == null
                ? List.of()
                : lesson.getTestCases().stream()
                .map(tc -> new PublicTestCaseResponseDTO(
                        tc.getId(),
                        tc.getStdin(),
                        tc.getExpectedStdout(),
                        tc.getIsHidden(),
                        tc.getOrderIndex(),
                        tc.getExplanation()))
                .toList();
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
                publicTestCases,
                null
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
