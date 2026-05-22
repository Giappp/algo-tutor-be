package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptResponse;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptSummary;
import org.rap.algotutorbe.learning.dto.quiz.QuizSubmitAnswer;
import org.rap.algotutorbe.learning.mapper.QuizAttemptMapper;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.QuizAttemptRepository;
import org.rap.algotutorbe.learning.repositories.QuizLessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizLessonRepository quizLessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptMapper quizAttemptMapper;

    public QuizAttemptResponse submitQuiz(String lessonSlug, QuizSubmitAnswer payload, SecurityUser authentication) {
        QuizLesson quizLesson = getOrThrow(lessonSlug);
        QuizAttempt quizAttempt = new QuizAttempt();
        quizAttempt.setUser(authentication.getUser());
        quizAttempt.setQuiz(quizLesson);
        quizAttempt.setRoadmapSlug(quizLesson.getTopic().getLearningPath().getSlug());
        quizAttempt.setLessonSlug(quizLesson.getSlug());
        quizAttempt.setStartedAt(payload.startedAt());
        quizAttempt.setTimeSpentSeconds(payload.timeSpentSeconds());
        quizAttempt.setCompletedAt(payload.completedAt());
        quizAttempt.setAttemptNumber(quizAttemptRepository.getNextAttemptNumber(quizLesson.getId()));
        quizAttempt.setAnswersSnapshot(payload.answers());
        AtomicInteger correctCount = new AtomicInteger();
        List<QuestionResult> questionResults = new ArrayList<>();
        payload.answers()
                .forEach(questionAnswer -> {
                    QuizQuestion quizQuestion = quizQuestionRepository.findById(questionAnswer.questionId())
                            .orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));
                    List<String> correctOptionIds = quizQuestion.getChoices().stream()
                            .filter(QuizChoice::getIsCorrect)
                            .map(QuizChoice::getId)
                            .toList();
                    boolean isCorrect = judge(quizQuestion, questionAnswer);
                    if (isCorrect) {
                        correctCount.getAndIncrement();
                    }
                    QuestionResult questionResult = new QuestionResult(
                            quizQuestion.getId(),
                            isCorrect, correctOptionIds);
                    questionResults.add(questionResult);
                });
        float point = correctCount.floatValue() / quizLesson.getQuestions().size();
        quizAttempt.setScore(point);
        quizAttempt.setCorrectCount(correctCount.get());
        quizAttempt.setQuestionResults(questionResults);
        quizAttempt.setTotalQuestions(quizLesson.getQuestions().size());
        quizAttempt.setPassed(Float.compare(quizLesson.getPassingScore(), point) <= 0);
        QuizAttempt saved = quizAttemptRepository.save(quizAttempt);
        return quizAttemptMapper.toResponse(saved);
    }

    private boolean judge(QuizQuestion quizQuestion, QuestionAnswer questionAnswer) {
        if (quizQuestion.getType() == QuestionType.MULTIPLE_CHOICE) {
            Set<String> correctAnswer = quizQuestion.getChoices()
                    .stream().filter(QuizChoice::getIsCorrect)
                    .map(QuizChoice::getId)
                    .collect(Collectors.toSet());
            return correctAnswer.containsAll(questionAnswer.selectedOptionIds());
        }
        if (quizQuestion.getType() == QuestionType.SINGLE_CHOICE && questionAnswer.selectedOptionIds().size() == 1) {
            QuizChoice correctQuizChoice = quizQuestion.getChoices()
                    .stream()
                    .filter(QuizChoice::getIsCorrect)
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));
            return questionAnswer.selectedOptionIds().getFirst().equals(correctQuizChoice.getId());
        }

        return false;
    }

    private QuizLesson getOrThrow(String slug) {
        return quizLessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    public List<QuizAttemptSummary> getAllQuizSummary(String lessonSlug, SecurityUser authentication) {
        return quizAttemptRepository.getQuizAttemptByUserAndLessonSlug(authentication.getUser(), lessonSlug)
                .stream()
                .map(quizAttemptMapper::toSummary)
                .toList();
    }
}
