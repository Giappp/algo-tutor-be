package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.QuizChoiceRequestDTO;
import org.rap.algotutorbe.learning.dto.QuizQuestionDTO;
import org.rap.algotutorbe.learning.dto.QuizQuestionResponseDTO;
import org.rap.algotutorbe.learning.mapper.QuizChoiceMapper;
import org.rap.algotutorbe.learning.mapper.QuizQuestionMapper;
import org.rap.algotutorbe.learning.models.QuizChoice;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.QuizQuestion;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizChoiceRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final LessonRepository lessonRepository;
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizChoiceMapper quizChoiceMapper;

    @Transactional
    public @Nullable ApiResponse<Object> addQuestion(Long quizLessonId, @Valid QuizQuestionDTO request) {
        QuizLesson quiz = getQuizLessonOrThrow(quizLessonId);

        QuizQuestion question = quizQuestionMapper.toEntity(request);
        question.setQuiz(quiz);
        question.setOrderIndex(quiz.getQuestions().size() + 1);

        if (request.choices() != null) {
            for (QuizChoiceRequestDTO choiceDto : request.choices()) {
                QuizChoice choice = quizChoiceMapper.toEntity(choiceDto);
                choice.setQuestion(question);
                question.getChoices().add(choice);
            }
        }

        quiz.getQuestions().add(question);
        QuizQuestion saved = quizQuestionRepository.save(question);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponse(saved));
    }

    @Transactional
    public @Nullable ApiResponse<Object> updateQuestion(Long questionId, @Valid QuizQuestionDTO request) {
        QuizQuestion question = getOrThrow(questionId);
        quizQuestionMapper.updateEntity(question, request);

        if (request.choices() != null) {
            question.getChoices().clear();
            for (QuizChoiceRequestDTO choiceDto : request.choices()) {
                QuizChoice choice = quizChoiceMapper.toEntity(choiceDto);
                choice.setQuestion(question);
                question.getChoices().add(choice);
            }
        }

        QuizQuestion saved = quizQuestionRepository.save(question);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponse(saved));
    }

    @Transactional
    public @Nullable ApiResponse<Object> deleteQuestion(Long questionId) {
        QuizQuestion question = getOrThrow(questionId);
        quizQuestionRepository.delete(question);
        return ApiResponse.buildMessage("Question deleted successfully");
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getQuestionsByQuizLessonId(Long quizLessonId) {
        getQuizLessonOrThrow(quizLessonId);
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndex(quizLessonId);
        List<QuizQuestionResponseDTO> responses = questions.stream()
                .map(q -> new QuizQuestionResponseDTO(
                        q.getId(),
                        q.getQuestion(),
                        q.getType(),
                        q.getPoints(),
                        q.getExplanation(),
                        q.getOrderIndex(),
                        q.getChoices().stream()
                                .map(c -> new org.rap.algotutorbe.learning.dto.QuizChoiceResponseDTO(
                                        c.getId(),
                                        c.getText(),
                                        c.getExplanation()))
                                .toList()))
                .toList();
        return ApiResponse.buildSuccess(responses);
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
}
