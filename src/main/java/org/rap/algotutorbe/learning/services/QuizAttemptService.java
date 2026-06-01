package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.rap.algotutorbe.judge.LessonProgressUpdater;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptResponse;
import org.rap.algotutorbe.learning.dto.quiz.QuizAttemptSummary;
import org.rap.algotutorbe.learning.dto.quiz.QuizSubmitAnswer;
import org.rap.algotutorbe.learning.mapper.QuizAttemptMapper;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.QuizAttemptRepository;
import org.rap.algotutorbe.learning.repositories.QuizLessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAttemptService {
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizLessonRepository quizLessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizAttemptMapper quizAttemptMapper;
    private final LessonProgressUpdater lessonProgressUpdater;

    @Transactional
    public QuizAttemptResponse submitQuiz(
            String lessonSlug,
            QuizSubmitAnswer payload,
            SecurityUser authentication
    ) {
        QuizLesson quizLesson = getOrThrow(lessonSlug);

        ScoreResult scoreResult = gradeQuiz(quizLesson, payload.answers());

        QuizAttempt quizAttempt = new QuizAttempt();
        quizAttempt.setUser(authentication.getUser());
        quizAttempt.setQuiz(quizLesson);
        quizAttempt.setRoadmapSlug(quizLesson.getTopic().getLearningPath().getSlug());
        quizAttempt.setLessonSlug(quizLesson.getSlug());
        quizAttempt.setStartedAt(payload.startedAt());
        quizAttempt.setCompletedAt(payload.completedAt());
        quizAttempt.setTimeSpentSeconds(payload.timeSpentSeconds());
        quizAttempt.setAttemptNumber(quizAttemptRepository.getNextAttemptNumber(quizLesson.getId()));
        quizAttempt.setAnswersSnapshot(payload.answers());

        quizAttempt.setScore(scoreResult.score());
        quizAttempt.setCorrectCount(scoreResult.correctCount());
        quizAttempt.setQuestionResults(scoreResult.questionResults());
        quizAttempt.setTotalQuestions(quizLesson.getQuestions().size());

        int passingScore = quizLesson.getPassingScore() != null
                ? quizLesson.getPassingScore()
                : 70;

        quizAttempt.setPassed(scoreResult.score() >= passingScore);

        QuizAttempt saved = quizAttemptRepository.save(quizAttempt);

        if (Boolean.TRUE.equals(saved.getPassed())) {
            try {
                lessonProgressUpdater.markLessonCompleted(authentication.getUser(), quizLesson);
            } catch (Exception e) {
                log.error(
                        "Failed to auto-update lesson progress for user [{}] on passed quiz [{}]",
                        authentication.getUser().getId(),
                        quizLesson.getSlug(),
                        e
                );
            }
        }

        return quizAttemptMapper.toResponse(saved);
    }

    private ScoreResult gradeQuiz(QuizLesson quizLesson, List<QuestionAnswer> submittedAnswers) {
        Map<Long, QuestionAnswer> answerByQuestionId = submittedAnswers == null
                ? Map.of()
                : submittedAnswers.stream()
                .collect(Collectors.toMap(
                        QuestionAnswer::questionId,
                        Function.identity(),
                        (first, duplicate) -> first
                ));

        int correctCount = 0;
        int earnedPoints = 0;
        int totalPoints = 0;

        List<QuestionResult> questionResults = new ArrayList<>();

        for (QuizQuestion question : quizLesson.getQuestions()) {
            int questionPoints = question.getPoints() != null ? question.getPoints() : 1;
            totalPoints += questionPoints;

            QuestionAnswer answer = answerByQuestionId.get(question.getId());

            boolean isCorrect = answer != null && judge(question, answer);

            if (isCorrect) {
                correctCount++;
                earnedPoints += questionPoints;
            }

            List<String> correctOptionIds = question.getChoices().stream()
                    .filter(choice -> Boolean.TRUE.equals(choice.getIsCorrect()))
                    .map(QuizChoice::getId)
                    .toList();

            questionResults.add(new QuestionResult(
                    question.getId(),
                    isCorrect,
                    correctOptionIds
            ));
        }

        float score = totalPoints == 0
                ? 0.0f
                : (earnedPoints * 100.0f) / totalPoints;

        return new ScoreResult(score, correctCount, questionResults);
    }

    private boolean judge(QuizQuestion quizQuestion, QuestionAnswer questionAnswer) {
        List<String> selected = questionAnswer.selectedOptionIds();

        log.info("Judging questionId={}", quizQuestion.getId());
        log.info("Selected option ids={}", selected);

        quizQuestion.getChoices().forEach(choice -> {
            log.info(
                    "Choice id={}, text={}, isCorrect={}",
                    choice.getId(),
                    choice.getText(),
                    choice.getIsCorrect()
            );
        });

        if (selected == null || selected.isEmpty()) {
            return false;
        }

        Set<String> correctAnswer = quizQuestion.getChoices()
                .stream()
                .filter(choice -> Boolean.TRUE.equals(choice.getIsCorrect()))
                .map(QuizChoice::getId)
                .collect(Collectors.toSet());

        log.info("Correct option ids={}", correctAnswer);

        Set<String> selectedSet = new HashSet<>(selected);
        log.info("Selected set={}", selectedSet);

        if (quizQuestion.getType() == QuestionType.MULTIPLE_CHOICE) {
            return selectedSet.equals(correctAnswer);
        }

        if (quizQuestion.getType() == QuestionType.SINGLE_CHOICE) {
            return selected.size() == 1 && correctAnswer.contains(selected.get(0));
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

    private record ScoreResult(
            float score,
            int correctCount,
            List<QuestionResult> questionResults
    ) {
    }
}
