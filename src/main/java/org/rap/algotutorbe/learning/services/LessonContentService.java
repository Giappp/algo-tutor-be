package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.landing.CodingContentResponse;
import org.rap.algotutorbe.learning.dto.landing.QuizContentResponse;
import org.rap.algotutorbe.learning.dto.landing.TheoryContentResponse;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonContentService {

    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public TheoryContentResponse getTheoryContent(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

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

        if (!(lesson instanceof QuizLesson quiz)) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }

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
                    question.getExplanation(),
                    correctOptionIds
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

    private int estimateReadingTime(String content) {
        if (content == null || content.isBlank()) return 1;
        int wordCount = content.split("\\s+").length;
        int minutes = (int) Math.ceil(wordCount / 200.0);
        return Math.max(1, minutes);
    }
}
