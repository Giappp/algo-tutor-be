package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.mapper.LessonMapper;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.rap.algotutorbe.learning.enums.VideoProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService extends BaseService {
    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final LessonMapper lessonMapper;
    private final SlugGenerator slugGenerator;

    @Transactional
    public ApiResponse<Object> create(Long topicId, @Valid LessonRequestDTO request) {
        Topic topic = getOrThrowTopic(topicId);

        if (request.getType() == null) {
            throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
        }
        Lesson lesson = buildLesson(request);
        lesson.setTopic(topic);
        lesson.setDisplayOrder(lessonRepository.getNextDisplayOrder(topicId));

        Lesson saved = lessonRepository.save(lesson);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(saved));
    }

    private Topic getOrThrowTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    @Transactional
    public ApiResponse<Object> update(Long lessonId, @Valid LessonRequestDTO request) {
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
            case VIDEO -> {
                if (!(lesson instanceof VideoLesson)) {
                    throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
                }
                lessonMapper.updateVideoFromDTO((VideoLessonRequestDTO) request, (VideoLesson) lesson);
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
        lesson.setEstimatedMinutes(request.getEstimatedMinutes());
    }

    @Transactional
    public ApiResponse<String> delete(Long lessonId) {
        Lesson lesson = getOrThrow(lessonId);
        lessonRepository.delete(lesson);
        return ApiResponse.buildMessage("Lesson deleted successfully");
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getById(Long lessonId) {
        Lesson lesson = getOrThrow(lessonId);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(lesson));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getBySlug(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(lesson));
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getByTopicId(Long topicId, Pageable pageable) {
        if (!topicRepository.existsById(topicId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }

        Page<Lesson> lessonPage = lessonRepository.findByTopicIdOrderByDisplayOrder(topicId, pageable);

        List<LessonResponseDTO> responses = lessonPage.getContent().stream()
                .map(lessonMapper::toResponse)
                .toList();
        return ApiResponse.buildSuccess(responses);
    }

    @Transactional
    public ApiResponse<Object> togglePublish(Long lessonId) {
        Lesson lesson = getOrThrow(lessonId);
        if (lesson instanceof VideoLesson video
                && !Boolean.TRUE.equals(lesson.getIsPublished())
                && video.getProcessingStatus() != VideoProcessingStatus.READY) {
            throw new AppException(ErrorCode.VIDEO_NOT_READY);
        }
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
            case VIDEO -> lesson = createVideoLesson((VideoLessonRequestDTO) request);
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
        return quiz;
    }

    private TheoryLesson createTheoryLesson(TheoryLessonRequestDTO request) {
        TheoryLesson theoryLesson = new TheoryLesson();
        theoryLesson.setTitle(request.getTitle());
        theoryLesson.setSlug(slugGenerator.generateUniqueForLesson(request.getTitle()));
        theoryLesson.setContent(request.getContent());
        theoryLesson.setEstimatedMinutes(request.getEstimatedMinutes());
        theoryLesson.setType(request.getType());
        theoryLesson.setDifficulty(request.getDifficulty());
        return theoryLesson;
    }

    private VideoLesson createVideoLesson(VideoLessonRequestDTO request) {
        VideoLesson videoLesson = new VideoLesson();
        videoLesson.setTitle(request.getTitle());
        videoLesson.setSlug(slugGenerator.generateUniqueForLesson(request.getTitle()));
        videoLesson.setDescription(request.getDescription());
        videoLesson.setType(request.getType());
        videoLesson.setDifficulty(request.getDifficulty());
        videoLesson.setProcessingStatus(VideoProcessingStatus.PENDING_UPLOAD);
        return videoLesson;
    }

    public Lesson getOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }

    public Lesson getOrThrowBySlug(String slug) {
        return lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }
}
