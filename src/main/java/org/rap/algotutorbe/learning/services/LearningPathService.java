package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.learning.dto.*;
import org.rap.algotutorbe.learning.mapper.LearningPathMapper;
import org.rap.algotutorbe.learning.mapper.QuestionType;
import org.rap.algotutorbe.learning.mapper.TopicMapper;
import org.rap.algotutorbe.learning.models.*;
import org.rap.algotutorbe.learning.repositories.LearningPathRepository;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningPathService extends BaseService {
    private final LearningPathRepository learningPathRepository;
    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LearningPathMapper learningPathMapper;
    private final TopicMapper topicMapper;
    private final SlugGenerator slugGenerator;

    @Transactional
    public @Nullable ApiResponse<Object> create(@Valid LearningPathRequestDTO request) {
        LearningPath learningPath = learningPathMapper.toEntity(request);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    @Transactional
    public @Nullable ApiResponse<Object> update(Long id, @Valid LearningPathRequestDTO request) {
        LearningPath learningPath = getOrThrow(id);
        learningPathMapper.updateEntity(learningPath, request);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    @Transactional
    public @Nullable ApiResponse<Object> addTopic(Long id, @Valid TopicRequestDTO request) {
        LearningPath learningPath = getOrThrow(id);
        Topic topic = topicMapper.toEntity(request);
        learningPath.addTopic(topic);
        LearningPath saved = learningPathRepository.save(learningPath);
        return ApiResponse.buildSuccess(saved);
    }

    @Transactional
    public @Nullable ApiResponse<Object> createLesson(Long topicId, LessonRequestDTO request) {
        Topic topic = getOrThrowTopic(topicId);
        Lesson lesson = buildLesson(request);
        topic.addLesson(lesson);
        Topic saved = topicRepository.save(topic);
        return ApiResponse.buildSuccess(saved);
    }

    private Lesson buildLesson(LessonRequestDTO request) {
        Lesson lesson;
        switch (request.getType()) {
            case THEORY -> lesson = createTheoryLesson(request);
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
        lesson.setContent(request.getContent());
        lesson.setOrderIndex(request.getOrderIndex());
        lesson.setType(request.getType());
        lesson.setDifficulty(request.getDifficulty());
        lesson.setStarterCode(request.getStarterCode());
        lesson.setConstraints(request.getConstraints() != null
                ? List.of(request.getConstraints().split("\n"))
                : new ArrayList<>());
        lesson.setHints(request.getHints() != null ? request.getHints() : new ArrayList<>());
        lesson.setExamples(request.getExamples() != null ? request.getExamples() : new ArrayList<>());
        lesson.setKeyInsights(request.getKeyInsights() != null ? request.getKeyInsights() : new ArrayList<>());
        lesson.setBaseTimeLimitMs(request.getTimeLimit() != null ? request.getTimeLimit() : 1000);
        lesson.setBaseMemoryLimitMb(request.getMemoryLimit() != null ? request.getMemoryLimit() : 256);

        if (request.getTestCases() != null) {
            List<Testcase> testCases = request.getTestCases().stream().map(dto -> {
                Testcase tc = new Testcase();
                tc.setStdin(dto.stdin());
                tc.setExpectedStdout(dto.expectedOutput());
                tc.setHidden(dto.isHidden() != null && dto.isHidden());
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
        quiz.setContent(request.getContent());
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
                    for (QuestionChoiceDTO c : q.choices()) {
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

    private TheoryLesson createTheoryLesson(LessonRequestDTO request) {
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
        while (lessonRepository.findBySlug(candidate).isPresent()) {
            candidate = baseSlug + "-" + counter++;
        }
        return candidate;
    }

    private LearningPath getOrThrow(Long id) {
        return learningPathRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEARNING_PATH_NOT_FOUND));
    }

    private Topic getOrThrowTopic(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }
}
