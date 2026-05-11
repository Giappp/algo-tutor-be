package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.mapper.LessonMapper;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final LessonMapper lessonMapper;
    private final SlugGenerator slugGenerator;

    @Transactional
    public @Nullable ApiResponse<Object> create(Long topicId, @Valid LessonRequestDTO request) {
        Topic topic = getOrThrowTopic(topicId);

        if (request.getType() == null) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }
        Lesson lesson = buildLesson(request);
        lesson.setTopic(topic);
        lesson.setOrderIndex(lessonRepository.getNextOrderIndex(topicId));
        lesson.setTopic(topic);

        Lesson saved = lessonRepository.save(lesson);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(saved));
    }

    private Topic getOrThrowTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    @Transactional
    public @Nullable ApiResponse<Object> update(Long lessonId, @Valid LessonRequestDTO request) {
        Lesson lesson = getOrThrow(lessonId);

        if (request.getType() == null) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }

        switch (request.getType()) {
            case THEORY -> {
                if (!(lesson instanceof TheoryLesson)) {
                    throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
                }
                updateTheory((TheoryLesson) lesson, (TheoryLessonRequestDTO) request);
            }
            case QUIZ -> {
                if (!(lesson instanceof QuizLesson)) {
                    throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
                }
                lessonMapper.updateQuizFromDTO((QuizLessonRequestDTO) request, (QuizLesson) lesson);
            }
            case CODING -> {
                if (!(lesson instanceof CodingLesson)) {
                    throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
                }
                lessonMapper.updateCodingFromDTO((CodingLessonRequestDTO) request, (CodingLesson) lesson);
            }
        }

        lesson.setTitle(request.getTitle());
        lesson.setSlug(slugGenerator.generateUniqueForLesson(request.getTitle()));
        lesson.setDifficulty(request.getDifficulty());

        Lesson saved = lessonRepository.save(lesson);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(saved));
    }

    private void updateTheory(TheoryLesson lesson, TheoryLessonRequestDTO request) {
        lesson.setContent(request.getContent());
    }

    @Transactional
    public @Nullable ApiResponse<String> delete(Long lessonId) {
        Lesson lesson = getOrThrow(lessonId);
        lessonRepository.delete(lesson);
        return ApiResponse.buildMessage("Lesson deleted successfully");
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getById(Long lessonId) {
        Lesson lesson = getOrThrow(lessonId);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(lesson));
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getPublishedById(Long lessonId) {
        Lesson lesson = getOrThrow(lessonId);
        if (!Boolean.TRUE.equals(lesson.getIsPublished())) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }
        return ApiResponse.buildSuccess(buildPublicResponse(lesson));
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getPublishedBySlug(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if (!Boolean.TRUE.equals(lesson.getIsPublished())) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }
        return ApiResponse.buildSuccess(buildPublicResponse(lesson));
    }

    private Object buildPublicResponse(Lesson lesson) {
        if (lesson instanceof CodingLesson coding) {
            return lessonMapper.toPublicCodingResponse(coding);
        }
        return lessonMapper.toDetailedResponse(lesson);
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getBySlug(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(lesson));
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getByTopicId(Long topicId, boolean publishedOnly, Pageable pageable) {
        if (!topicRepository.existsById(topicId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }

        Page<Lesson> lessonPage = publishedOnly
                ? lessonRepository.findByTopicIdAndPublishedTrueOrderByOrderIndex(topicId, pageable)
                : lessonRepository.findByTopicIdOrderByOrderIndex(topicId, pageable);

        List<LessonResponseDTO> responses = lessonPage.getContent().stream()
                .map(lessonMapper::toResponse)
                .toList();
        return ApiResponse.buildSuccess(responses);
    }

    @Transactional
    public @Nullable ApiResponse<Object> togglePublish(Long lessonId) {
        Lesson lesson = getOrThrow(lessonId);
        lesson.setIsPublished(!Boolean.TRUE.equals(lesson.getIsPublished()));
        Lesson saved = lessonRepository.save(lesson);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(saved));
    }

    private Lesson buildLesson(LessonRequestDTO request) {
        Lesson lesson;
        switch (request.getType()) {
            case THEORY -> lesson = createTheoryLesson((TheoryLessonRequestDTO) request);
            case QUIZ -> lesson = createQuizLesson((QuizLessonRequestDTO) request);
            case CODING -> lesson = createCodingLesson((CodingLessonRequestDTO) request);
            default -> throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }
        return lesson;
    }

    private CodingLesson createCodingLesson(CodingLessonRequestDTO request) {
        CodingLesson lesson = lessonMapper.toEntity(request);
        lesson.setSlug(slugGenerator.generateUniqueForLesson(request.getTitle()));
        lesson.setConstraints(request.getConstraints() != null
                ? request.getConstraints()
                : new ArrayList<>());
        lesson.setHints(request.getHints() != null ? request.getHints() : new ArrayList<>());
        lesson.setExamples(request.getExamples() != null ? request.getExamples() : new ArrayList<>());
        lesson.setBaseTimeLimitMs(request.getBaseTimeLimitMs() != null ? request.getBaseTimeLimitMs() : 2000);
        lesson.setBaseMemoryLimitMb(request.getBaseMemoryLimitMb() != null ? request.getBaseMemoryLimitMb() : 256);

        return lesson;
    }

    private QuizLesson createQuizLesson(QuizLessonRequestDTO request) {
        QuizLesson quiz = lessonMapper.toEntity(request);
        quiz.setSlug(slugGenerator.generateUniqueForLesson(request.getTitle()));
        AtomicInteger index = new AtomicInteger(1);
        quiz.getQuestions()
                .forEach(question -> {
                    question.setQuiz(quiz);
                    question.setOrderIndex(index.getAndIncrement());
                });
        return quiz;
    }

    private TheoryLesson createTheoryLesson(TheoryLessonRequestDTO request) {
        TheoryLesson theoryLesson = new TheoryLesson();
        theoryLesson.setTitle(request.getTitle());
        theoryLesson.setSlug(slugGenerator.generateUniqueForLesson(request.getTitle()));
        theoryLesson.setContent(request.getContent());
        theoryLesson.setType(request.getType());
        theoryLesson.setDifficulty(request.getDifficulty());
        return theoryLesson;
    }

    public Lesson getOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }
}
