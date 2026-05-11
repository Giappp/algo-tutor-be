package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.QuizQuestionDTO;
import org.rap.algotutorbe.learning.dto.QuizQuestionResponseDTO;
import org.rap.algotutorbe.learning.mapper.QuizQuestionMapper;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.QuizQuestion;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizQuestionRepository quizQuestionRepository;
    private final LessonRepository lessonRepository;
    private final QuizQuestionMapper quizQuestionMapper;

    @Transactional
    public @Nullable ApiResponse<QuizQuestionResponseDTO> addQuestion(Long quizLessonId, @Valid QuizQuestionDTO request) {
        QuizLesson quiz = getQuizLessonOrThrow(quizLessonId);

        QuizQuestion question = quizQuestionMapper.toEntity(request);
        question.setQuiz(quiz);
        question.setOrderIndex(quizQuestionRepository.findNextOrderIndex(quizLessonId));

        quiz.getQuestions().add(question);
        QuizQuestion saved = quizQuestionRepository.save(question);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponse(saved));
    }

    @Transactional
    public @Nullable ApiResponse<QuizQuestionResponseDTO> updateQuestion(Long questionId, @Valid QuizQuestionDTO request) {
        QuizQuestion question = getOrThrow(questionId);
        quizQuestionMapper.updateEntity(question, request);
        QuizQuestion saved = quizQuestionRepository.save(question);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponse(saved));
    }

    @Transactional
    public @Nullable ApiResponse<String> deleteQuestion(Long questionId) {
        QuizQuestion question = getOrThrow(questionId);
        quizQuestionRepository.delete(question);
        return ApiResponse.buildMessage("Question deleted successfully");
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<List<QuizQuestionResponseDTO>> getQuestionsByQuizLessonId(Long quizLessonId) {
        getQuizLessonOrThrow(quizLessonId);
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndex(quizLessonId);
        return ApiResponse.buildSuccess(quizQuestionMapper.toResponseList(questions));
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
