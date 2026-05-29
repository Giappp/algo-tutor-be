package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.landing.CodingContentResponse;
import org.rap.algotutorbe.learning.dto.landing.QuizContentResponse;
import org.rap.algotutorbe.learning.dto.landing.TheoryContentResponse;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.EnrollmentRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonContentService extends BaseService {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public TheoryContentResponse getTheoryContent(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        validateLessonAccess(lesson);

        if (!(lesson instanceof TheoryLesson theory)) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }

        int estimatedMinutes = theory.getEstimatedMinutes() != null
                ? theory.getEstimatedMinutes()
                : estimateReadingTime(theory.getContent());

        return new TheoryContentResponse(
                theory.getId(),
                theory.getSlug(),
                theory.getTitle(),
                theory.getContent(),
                estimatedMinutes
        );
    }

    @Transactional(readOnly = true)
    public QuizContentResponse getQuizContent(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        validateLessonAccess(lesson);

        if (!(lesson instanceof QuizLesson quiz)) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }

        SecurityUser currentUser = getCurrentUser().orElse(null);
        boolean isAdminOrEditor = currentUser != null &&
                ("ADMIN".equals(currentUser.getRoleCode()) || "EDITOR".equals(currentUser.getRoleCode()));

        List<QuizContentResponse.QuizQuestionItem> questionItems = new ArrayList<>();

        for (QuizQuestion question : quiz.getQuestions()) {
            List<QuizContentResponse.QuizOptionItem> options = new ArrayList<>();
            List<String> correctOptionIds = new ArrayList<>();

            List<QuizChoice> choices = question.getChoices();
            for (int i = 0; i < choices.size(); i++) {
                QuizChoice choice = choices.get(i);
                // Use stored id if available, otherwise generate from index (a, b, c...)
                String optionId = choice.getId() != null ? choice.getId()
                        : String.valueOf((char) ('a' + i));
                options.add(new QuizContentResponse.QuizOptionItem(optionId, choice.getText()));
                if (Boolean.TRUE.equals(choice.getIsCorrect())) {
                    correctOptionIds.add(optionId);
                }
            }

            questionItems.add(new QuizContentResponse.QuizQuestionItem(
                    question.getId(),
                    question.getQuestion(),
                    question.getType().name(),
                    options,
                    isAdminOrEditor ? question.getExplanation() : null,
                    isAdminOrEditor ? correctOptionIds : List.of()
            ));
        }

        return new QuizContentResponse(
                quiz.getId(),
                quiz.getSlug(),
                quiz.getTitle(),
                quiz.getPassingScore(),
                questionItems
        );
    }

    @Transactional(readOnly = true)
    public CodingContentResponse getCodingContent(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        validateLessonAccess(lesson);

        if (!(lesson instanceof CodingLesson coding)) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }

        List<CodingContentResponse.TestCaseItem> testCaseItems = new ArrayList<>();

        return new CodingContentResponse(
                coding.getId(),
                coding.getSlug(),
                coding.getTitle(),
                coding.getStatement(),
                coding.getStarterCode(),
                testCaseItems,
                coding.getHints(),
                coding.getBaseTimeLimitMs(),
                coding.getBaseMemoryLimitMb()
        );
    }

    private void validateLessonAccess(Lesson lesson) {
        SecurityUser currentUser = getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.NEED_AUTHENTICATION));

        String role = currentUser.getRoleCode();
        if ("ADMIN".equals(role) || "EDITOR".equals(role)) {
            return;
        }

        if (lesson.getTopic() == null || lesson.getTopic().getLearningPath() == null) {
            throw new AppException(ErrorCode.TOPIC_NOT_IN_LEARNING_PATH);
        }

        Long learningPathId = lesson.getTopic().getLearningPath().getId();
        UUID userId = currentUser.getId();

        boolean isEnrolled = enrollmentRepository.existsByUserIdAndLearningPathId(userId, learningPathId);
        if (!isEnrolled) {
            throw new AppException(ErrorCode.NOT_ENROLLED);
        }
    }

    private int estimateReadingTime(String content) {
        if (content == null || content.isBlank()) return 1;
        int wordCount = content.split("\\s+").length;
        int minutes = (int) Math.ceil(wordCount / 200.0);
        return Math.max(1, minutes);
    }
}
