package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.QuizQuestionDTO;
import org.rap.algotutorbe.learning.dto.QuizQuestionResponseDTO;
import org.rap.algotutorbe.learning.mapper.QuizQuestionMapper;
import org.rap.algotutorbe.learning.models.QuizChoice;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.QuizQuestion;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizService {
    private static final List<String> DEFAULT_CHOICE_IDS = List.of(
            "a", "b", "c", "d", "e", "f", "g", "h"
    );

    private final QuizQuestionRepository quizQuestionRepository;
    private final LessonRepository lessonRepository;
    private final QuizQuestionMapper quizQuestionMapper;

    @Transactional
    public ApiResponse<QuizQuestionResponseDTO> addQuestion(Long quizLessonId, @Valid QuizQuestionDTO request) {
        QuizLesson quiz = getQuizLessonOrThrow(quizLessonId);
        QuizQuestion question = quizQuestionMapper.toEntity(request);
        question.setQuiz(quiz);
        question.setOrderIndex(quizQuestionRepository.findNextOrderIndex(quizLessonId));
        question.setChoices(normalizeChoices(question.getChoices()));
        quiz.getQuestions().add(question);
        validateChoices(question);
        QuizQuestion saved = quizQuestionRepository.save(question);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponse(saved));
    }

    @Transactional
    public ApiResponse<QuizQuestionResponseDTO> updateQuestion(Long questionId, @Valid QuizQuestionDTO request) {
        QuizQuestion question = getOrThrow(questionId);

        quizQuestionMapper.updateEntity(question, request);
        question.setChoices(normalizeChoices(question.getChoices()));

        QuizQuestion saved = quizQuestionRepository.save(question);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponse(saved));
    }

    @Transactional
    public ApiResponse<String> deleteQuestion(Long questionId) {
        QuizQuestion question = getOrThrow(questionId);
        quizQuestionRepository.delete(question);
        return ApiResponse.buildMessage("Question deleted successfully");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<QuizQuestionResponseDTO>> getQuestionsByQuizLessonId(Long quizLessonId) {
        getQuizLessonOrThrow(quizLessonId);
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndex(quizLessonId);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponseList(questions));
    }

    private List<QuizChoice> normalizeChoices(List<QuizChoice> choices) {
        if (choices == null || choices.isEmpty()) {
            return new ArrayList<>();
        }

        for (int i = 0; i < choices.size(); i++) {
            QuizChoice choice = choices.get(i);

            if (choice.getId() == null || choice.getId().isBlank()) {
                String choiceId = i < DEFAULT_CHOICE_IDS.size()
                        ? DEFAULT_CHOICE_IDS.get(i)
                        : UUID.randomUUID().toString();

                choice.setId(choiceId);
            }

            if (choice.getIsCorrect() == null) {
                choice.setIsCorrect(false);
            }
        }

        return choices;
    }

    private QuizLesson getQuizLessonOrThrow(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .filter(QuizLesson.class::isInstance)
                .map(l -> (QuizLesson) l)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_LESSON_REQUIRED));
    }

    private QuizQuestion getOrThrow(Long id) {
        return quizQuestionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));
    }

    private void validateChoices(QuizQuestion question) {
        List<QuizChoice> choices = question.getChoices();

        if (choices == null || choices.size() < 2) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }

        long correctCount = choices.stream()
                .filter(choice -> Boolean.TRUE.equals(choice.getIsCorrect()))
                .count();

        switch (question.getType()) {
            case SINGLE_CHOICE -> {
                if (correctCount != 1) {
                    throw new AppException(ErrorCode.INVALID_PAYLOAD);
                }
            }
            case MULTIPLE_CHOICE -> {
                if (correctCount < 1) {
                    throw new AppException(ErrorCode.INVALID_PAYLOAD);
                }
            }
        }
    }
}