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
import org.rap.algotutorbe.learning.mapper.QuestionType;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

        Lesson lesson = buildLesson(request);
        topic.getLessons().add(lesson);
        lesson.setTopic(topic);
        lesson.setOrderIndex(topic.getLessons().size());

        topicRepository.save(topic);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(lesson));
    }

    private Topic getOrThrowTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    @Transactional
    public @Nullable ApiResponse<Object> update(Long lessonId, @Valid LessonRequestDTO request) {
        Lesson lesson = getOrThrow(lessonId);

        switch (request.getType()) {
            case THEORY -> {
                if (!(lesson instanceof TheoryLesson)) {
                    throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
                }
                lessonMapper.updateFromRequest(request, lesson);
            }
            case QUIZ -> {
                if (!(lesson instanceof QuizLesson)) {
                    throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
                }
                lessonMapper.updateFromRequest(request, lesson);
            }
            case CODING -> {
                if (!(lesson instanceof CodingLesson)) {
                    throw new AppException(ErrorCode.INVALID_LESSON_TYPE);
                }
                lessonMapper.updateFromRequest(request, lesson);
            }
        }

        if (request.getTitle() != null) {
            lesson.setTitle(request.getTitle());
        }
        if (request.getDifficulty() != null) {
            lesson.setDifficulty(request.getDifficulty());
        }

        Lesson saved = lessonRepository.save(lesson);
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(saved));
    }

    @Transactional
    public @Nullable ApiResponse<Object> delete(Long lessonId) {
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
    public @Nullable ApiResponse<Object> getBySlug(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        return ApiResponse.buildSuccess(lessonMapper.toDetailedResponse(lesson));
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getByTopicId(Long topicId, boolean publishedOnly) {
        if (!topicRepository.existsById(topicId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }
        List<Lesson> lessons = publishedOnly
                ? lessonRepository.findByTopicIdAndPublishedTrueOrderByOrderIndex(topicId)
                : lessonRepository.findByTopicIdOrderByOrderIndex(topicId);
        List<LessonResponseDTO> responses = lessons.stream()
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
        CodingLesson lesson = new CodingLesson();
        lesson.setTitle(request.getTitle());
        lesson.setSlug(generateUniqueSlug(request.getTitle()));
        lesson.setStatement(request.getStatement());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setType(request.getType());
        lesson.setDifficulty(request.getDifficulty());
        lesson.setStarterCode(request.getStarterCode());
        lesson.setConstraints(request.getConstraints() != null
                ? request.getConstraints()
                : new ArrayList<>());
        lesson.setHints(request.getHints() != null ? request.getHints() : new ArrayList<>());
        lesson.setExamples(request.getExamples() != null ? request.getExamples() : new ArrayList<>());
        lesson.setKeyInsights(request.getKeyInsights() != null ? request.getKeyInsights() : new ArrayList<>());
        lesson.setBaseTimeLimitMs(request.getBaseTimeLimitMs() != null ? request.getBaseTimeLimitMs() : 2000);
        lesson.setBaseMemoryLimitMb(request.getBaseMemoryLimitMb() != null ? request.getBaseMemoryLimitMb() : 256);

        if (request.getTestCases() != null) {
            List<Testcase> testCases = request.getTestCases().stream().map(dto -> {
                Testcase tc = new Testcase();
                tc.setStdin(dto.stdin());
                tc.setExpectedStdout(dto.expectedStdout());
                tc.setIsHidden(dto.isHidden() != null && dto.isHidden());
                tc.setOrderIndex(dto.orderIndex() != null ? dto.orderIndex() : 0);
                tc.setExplanation(dto.explanation());
                tc.setCodingLesson(lesson);
                return tc;
            }).toList();
            lesson.setTestCases(testCases);
        }

        return lesson;
    }

    private QuizLesson createQuizLesson(QuizLessonRequestDTO request) {
        QuizLesson quiz = new QuizLesson();
        quiz.setTitle(request.getTitle());
        quiz.setSlug(generateUniqueSlug(request.getTitle()));
        quiz.setOrderIndex(request.getOrderIndex());
        quiz.setType(request.getType());
        quiz.setDifficulty(request.getDifficulty());
        quiz.setPassingScore(request.getPassingScore() != null ? request.getPassingScore() : 70);
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());

        if (request.getQuestions() != null) {
            List<QuizQuestion> questions = new ArrayList<>();
            for (QuizQuestionDTO q : request.getQuestions()) {
                QuizQuestion question = new QuizQuestion();
                question.setQuiz(quiz);
                question.setQuestion(q.question());
                question.setType(q.type() != null ? q.type() : QuestionType.SINGLE_CHOICE);
                question.setPoints(q.points() != null ? q.points() : 1);
                question.setOrderIndex(questions.size() + 1);
                question.setExplanation(q.explanation());
                if (q.choices() != null) {
                    for (QuizChoiceRequestDTO c : q.choices()) {
                        QuizChoice choice = new QuizChoice();
                        choice.setText(c.text());
                        choice.setIsCorrect(c.isCorrect() != null && c.isCorrect());
                        choice.setExplanation(c.explanation());
                        question.getChoices().add(choice);
                    }
                }
                questions.add(question);
            }
            quiz.setQuestions(questions);
        }

        return quiz;
    }

    private TheoryLesson createTheoryLesson(TheoryLessonRequestDTO request) {
        TheoryLesson theoryLesson = new TheoryLesson();
        theoryLesson.setTitle(request.getTitle());
        theoryLesson.setSlug(generateUniqueSlug(request.getTitle()));
        theoryLesson.setContent(request.getContent());
        theoryLesson.setOrderIndex(request.getOrderIndex());
        theoryLesson.setType(request.getType());
        theoryLesson.setDifficulty(request.getDifficulty());
        return theoryLesson;
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = slugGenerator.generateFrom(title);
        String candidate = baseSlug;
        int counter = 1;
        while (lessonRepository.existsBySlug(candidate)) {
            candidate = baseSlug + "-" + counter++;
        }
        return candidate;
    }

    public Lesson getOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    }
}
