package org.rap.algotutorbe.ai.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AdminLessonContentGenerateRequest;
import org.rap.algotutorbe.ai.dto.AdminLessonContentGenerateResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLessonContentGenerationService {

    private static final int MAX_EXISTING_CONTENT_LENGTH = 15_000;

    private final LessonRepository lessonRepository;
    private final AiLlmExecutor aiLlmExecutor;
    private final ObjectMapper objectMapper;
    private final MarkdownLessonRequestBuilder markdownLessonRequestBuilder;

    @Transactional(readOnly = true)
    public AdminLessonContentGenerateResponse generate(
            Long lessonId,
            AdminLessonContentGenerateRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Topic topic = lesson.getTopic();
        LearningPath learningPath = topic.getLearningPath();
        List<String> siblingLessons = topic.getLessons().stream()
                .sorted(Comparator.comparing(Lesson::getDisplayOrder))
                .map(sibling -> sibling.getTitle() + " (" + sibling.getType() + ")")
                .toList();

        List<Message> messages = List.of(
                new SystemMessage(buildSystemPrompt(lesson.getType())),
                new UserMessage(buildUserPrompt(learningPath, topic, lesson, siblingLessons, request.prompt())));

        AiLlmExecutor.ChatResponseWithTokens llmResult =
                aiLlmExecutor.callWithFallback(request.provider(), messages, null);
        Object generatedContent =
                markdownLessonRequestBuilder.build(lesson, llmResult.responseText());

        return new AdminLessonContentGenerateResponse(
                lesson.getId(),
                lesson.getType(),
                generatedContent,
                new AdminLessonContentGenerateResponse.GenerationContext(
                        learningPath.getId(),
                        learningPath.getName(),
                        topic.getId(),
                        topic.getName(),
                        siblingLessons),
                llmResult.inputTokens(),
                llmResult.outputTokens());
    }

    private String buildSystemPrompt(LessonType lessonType) {
        return """
                You are an expert curriculum author for an algorithms and data structures learning platform.
                Generate a draft for an admin to review. Use the supplied curriculum context to keep the lesson
                aligned with its learning path, topic, difficulty, and neighboring lessons.

                Return only raw Markdown. Do not return JSON. Do not wrap the whole response in a Markdown code fence.
                Do not add commentary before or after the lesson content.

                %s
                """.formatted(markdownInstructionFor(lessonType));
    }

    private String markdownInstructionFor(LessonType lessonType) {
        return switch (lessonType) {
            case THEORY -> """
                    Write a complete theory lesson using headings, explanations, examples, key takeaways,
                    and common mistakes.
                    """;
            case CODING -> """
                    Write a complete coding problem statement using headings for description, input/output,
                    constraints, and examples. Do not include a solution, editorial, test cases, or full answer.
                    """;
            case QUIZ -> """
                    Use exactly this Markdown structure for every question:

                    ## Question 1
                    Question text
                    - [x] Correct choice
                    - [ ] Incorrect choice
                    - [ ] Another incorrect choice
                    > Explanation: Explanation shown after answering

                    Use more than one `[x]` choice only for multiple-choice questions.
                    Every question must have at least two choices and at least one correct choice.
                    """;
            case VIDEO -> """
                    Write a structured video lesson script using headings for learning objectives, narration,
                    visual demonstrations, examples, recap, and suggested next steps.
                    """;
        };
    }

    private String buildUserPrompt(
            LearningPath learningPath,
            Topic topic,
            Lesson lesson,
            List<String> siblingLessons,
            String adminPrompt) {
        return """
                [CURRICULUM_CONTEXT]
                Learning path: %s
                Level: %s
                Learning path description: %s
                Learning path goal: %s
                Topic: %s
                Topic description: %s
                Lessons in this topic: %s
                Target lesson title: %s
                Target lesson type: %s
                Target lesson difficulty: %s
                Existing target content:
                %s
                [/CURRICULUM_CONTEXT]

                [ADMIN_INSTRUCTION]
                %s
                [/ADMIN_INSTRUCTION]
                """.formatted(
                learningPath.getName(),
                learningPath.getLevel(),
                valueOrEmpty(learningPath.getDescription()),
                valueOrEmpty(learningPath.getGoal()),
                topic.getName(),
                valueOrEmpty(topic.getDescription()),
                siblingLessons,
                lesson.getTitle(),
                lesson.getType(),
                lesson.getDifficulty(),
                truncate(existingContent(lesson)),
                adminPrompt);
    }

    private String existingContent(Lesson lesson) {
        try {
            if (lesson instanceof TheoryLesson theoryLesson) {
                return theoryLesson.getContent();
            }
            if (lesson instanceof CodingLesson codingLesson) {
                return objectMapper.writeValueAsString(codingLessonContent(codingLesson));
            }
            if (lesson instanceof QuizLesson quizLesson) {
                return objectMapper.writeValueAsString(quizLessonContent(quizLesson));
            }
            return "";
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize existing lesson content for lesson [{}]", lesson.getId(), e);
            return "";
        }
    }

    private Object codingLessonContent(CodingLesson lesson) {
        return new CodingContent(
                lesson.getStatement(),
                lesson.getBaseTimeLimitMs(),
                lesson.getBaseMemoryLimitMb(),
                lesson.getConstraints(),
                lesson.getStarterCode(),
                lesson.getExamples(),
                lesson.getHints());
    }

    private Object quizLessonContent(QuizLesson lesson) {
        return lesson.getQuestions().stream()
                .map(question -> new QuizQuestionContent(
                        question.getQuestion(),
                        question.getType(),
                        question.getPoints(),
                        question.getExplanation(),
                        question.getChoices()))
                .toList();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_EXISTING_CONTENT_LENGTH) {
            return valueOrEmpty(value);
        }
        return value.substring(0, MAX_EXISTING_CONTENT_LENGTH);
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private record CodingContent(
            String statement,
            Integer baseTimeLimitMs,
            Integer baseMemoryLimitMb,
            Object constraints,
            Object starterCode,
            Object examples,
            Object hints
    ) {
    }

    private record QuizQuestionContent(
            String question,
            Object type,
            Integer points,
            String explanation,
            Object choices
    ) {
    }
}
