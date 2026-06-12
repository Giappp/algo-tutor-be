package org.rap.algotutorbe.ai.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiQuestionSourceResponse;
import org.rap.algotutorbe.ai.dto.GenerateQuestionsFromSourcesRequest;
import org.rap.algotutorbe.ai.dto.GenerateQuestionsFromSourcesResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.Lesson;
import org.rap.algotutorbe.learning.models.QuestionType;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAiQuizQuestionService {

    private static final int CONTENT_BUDGET = 30_000;
    private static final int PREVIEW_LENGTH = 180;
    private static final int MIN_POINTS = 1;
    private static final int MAX_POINTS = 10;
    private static final Pattern SCRIPT_OR_STYLE =
            Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern HTML_TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final LessonRepository lessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final AiLlmExecutor aiLlmExecutor;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<AiQuestionSourceResponse> getQuestionSources(Long quizLessonId) {
        QuizLesson quiz = getQuiz(quizLessonId);
        return lessonRepository.findQuestionSourcesByLearningPathId(learningPathId(quiz)).stream()
                .map(this::toSourceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GenerateQuestionsFromSourcesResponse generateQuestions(
            Long quizLessonId,
            GenerateQuestionsFromSourcesRequest request) {
        QuizLesson quiz = getQuiz(quizLessonId);
        List<TheoryLesson> sources = selectSources(quiz, request.sourceLessonIds());
        ContextBuildResult context = buildContext(sources);
        List<String> existingQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndex(quizLessonId).stream()
                .map(question -> question.getQuestion())
                .toList();

        List<Message> messages = generationMessages(quiz, request, context.content(), existingQuestions);
        AiLlmExecutor.ChatResponseWithTokens first =
                aiLlmExecutor.callWithFallback(providerName(request), messages, null);

        List<GenerateQuestionsFromSourcesResponse.DraftQuestion> questions;
        AiLlmExecutor.ChatResponseWithTokens result = first;
        try {
            questions = parseAndValidate(first.responseText(), request);
        } catch (AppException invalidFirstOutput) {
            log.warn("Invalid generated quiz questions for quiz [{}]; attempting one repair", quizLessonId);
            result = aiLlmExecutor.callWithFallback(
                    providerName(request),
                    repairMessages(first.responseText(), request),
                    null);
            questions = parseAndValidate(result.responseText(), request);
            result = sumUsage(first, result);
        }

        return new GenerateQuestionsFromSourcesResponse(
                quizLessonId,
                questions,
                new GenerateQuestionsFromSourcesResponse.GenerationContext(
                        sources.stream().map(this::toContextSource).toList(),
                        context.truncatedSourceIds()),
                result.inputTokens(),
                result.outputTokens());
    }

    private QuizLesson getQuiz(Long quizLessonId) {
        Lesson lesson = lessonRepository.findById(quizLessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if (!(lesson instanceof QuizLesson quiz)) {
            throw new AppException(ErrorCode.QUIZ_LESSON_REQUIRED);
        }
        return quiz;
    }

    private List<TheoryLesson> selectSources(QuizLesson quiz, List<Long> sourceIds) {
        if (new HashSet<>(sourceIds).size() != sourceIds.size()) {
            throw new AppException(ErrorCode.INVALID_AI_QUESTION_SOURCES);
        }

        Map<Long, TheoryLesson> available = new LinkedHashMap<>();
        lessonRepository.findQuestionSourcesByLearningPathId(learningPathId(quiz))
                .forEach(source -> available.put(source.getId(), source));

        if (!available.keySet().containsAll(sourceIds)) {
            throw new AppException(ErrorCode.INVALID_AI_QUESTION_SOURCES);
        }

        return available.values().stream()
                .filter(source -> sourceIds.contains(source.getId()))
                .toList();
    }

    private ContextBuildResult buildContext(List<TheoryLesson> sources) {
        int perSourceBudget = Math.max(1, CONTENT_BUDGET / sources.size());
        List<Long> truncated = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        for (TheoryLesson source : sources) {
            String cleaned = cleanContent(source.getContent());
            if (cleaned.length() > perSourceBudget) {
                cleaned = cleaned.substring(0, perSourceBudget);
                truncated.add(source.getId());
            }
            content.append("\n[SOURCE lessonId=").append(source.getId())
                    .append(" title=\"").append(source.getTitle()).append("\" topic=\"")
                    .append(source.getTopic().getName()).append("\"]\n")
                    .append(cleaned)
                    .append("\n[/SOURCE]\n");
        }
        return new ContextBuildResult(content.toString(), truncated);
    }

    private List<Message> generationMessages(
            QuizLesson quiz,
            GenerateQuestionsFromSourcesRequest request,
            String sourceContent,
            List<String> existingQuestions) {
        String systemPrompt = """
                You generate grounded draft quiz questions for an algorithms learning platform.
                Use only facts present in the supplied SOURCE blocks. Do not add outside knowledge.
                Return only valid JSON matching this shape:
                {"questions":[{"question":"...","type":"SINGLE_CHOICE","points":1,"orderIndex":1,
                "explanation":"... or null","choices":[{"text":"...","isCorrect":true,"explanation":"... or null"}]}]}
                Do not wrap JSON in Markdown fences or include commentary.
                """;
        String userPrompt = """
                Quiz title: %s
                Difficulty: %s
                Allowed question types: %s
                Exact question count: %d
                Exact choices per question: %d
                Include explanations: %s
                Admin instruction: %s
                Existing quiz questions to avoid duplicating: %s

                Rules:
                - Questions and choices must be non-empty and unique.
                - SINGLE_CHOICE must have exactly one correct choice.
                - MULTIPLE_CHOICE must have at least two correct choices.
                - points must be between %d and %d.
                - orderIndex must run from 1 to %d.

                %s
                """.formatted(
                quiz.getTitle(),
                request.difficulty(),
                request.questionTypes(),
                request.count(),
                request.choicesPerQuestion(),
                request.includeExplanations(),
                trimmedOrEmpty(request.prompt()),
                existingQuestions,
                MIN_POINTS,
                MAX_POINTS,
                request.count(),
                sourceContent);
        return List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt));
    }

    private List<Message> repairMessages(String invalidOutput, GenerateQuestionsFromSourcesRequest request) {
        return List.of(
                new SystemMessage("""
                        Repair the supplied JSON quiz draft. Return only valid JSON with the same schema.
                        Preserve grounded question meaning. Do not include Markdown or commentary.
                        """),
                new UserMessage("""
                        Required count: %d
                        Required choices per question: %d
                        Allowed types: %s
                        Include explanations: %s
                        Invalid output:
                        %s
                        """.formatted(
                        request.count(),
                        request.choicesPerQuestion(),
                        request.questionTypes(),
                        request.includeExplanations(),
                        invalidOutput)));
    }

    private List<GenerateQuestionsFromSourcesResponse.DraftQuestion> parseAndValidate(
            String responseText,
            GenerateQuestionsFromSourcesRequest request) {
        try {
            GeneratedPayload payload = objectMapper.readValue(stripJsonFence(responseText), GeneratedPayload.class);
            validatePayload(payload, request);
            return payload.questions();
        } catch (JsonProcessingException | RuntimeException e) {
            if (e instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.INVALID_AI_GENERATED_QUESTIONS, e);
        }
    }

    private void validatePayload(GeneratedPayload payload, GenerateQuestionsFromSourcesRequest request) {
        if (payload == null || payload.questions() == null || payload.questions().size() != request.count()) {
            invalidGeneratedQuestions();
        }

        Set<String> questionTexts = new HashSet<>();
        for (int index = 0; index < payload.questions().size(); index++) {
            GenerateQuestionsFromSourcesResponse.DraftQuestion question = payload.questions().get(index);
            if (question == null
                    || isBlank(question.question())
                    || question.type() == null
                    || !request.questionTypes().contains(question.type())
                    || question.points() == null
                    || question.points() < MIN_POINTS
                    || question.points() > MAX_POINTS
                    || question.orderIndex() == null
                    || question.orderIndex() != index + 1
                    || question.choices() == null
                    || question.choices().size() != request.choicesPerQuestion()
                    || (!request.includeExplanations() && !isBlank(question.explanation()))) {
                invalidGeneratedQuestions();
            }
            if (!questionTexts.add(normalizeForComparison(question.question()))) {
                invalidGeneratedQuestions();
            }

            int correct = 0;
            Set<String> choices = new HashSet<>();
            for (GenerateQuestionsFromSourcesResponse.DraftChoice choice : question.choices()) {
                if (choice == null
                        || isBlank(choice.text())
                        || choice.isCorrect() == null
                        || (!request.includeExplanations() && !isBlank(choice.explanation()))
                        || !choices.add(normalizeForComparison(choice.text()))) {
                    invalidGeneratedQuestions();
                }
                if (Boolean.TRUE.equals(choice.isCorrect())) {
                    correct++;
                }
            }
            if ((question.type() == QuestionType.SINGLE_CHOICE && correct != 1)
                    || (question.type() == QuestionType.MULTIPLE_CHOICE && correct < 2)) {
                invalidGeneratedQuestions();
            }
        }
    }

    private AiQuestionSourceResponse toSourceResponse(TheoryLesson source) {
        String cleaned = cleanContent(source.getContent());
        return new AiQuestionSourceResponse(
                source.getId(),
                source.getTitle(),
                source.getTopic().getId(),
                source.getTopic().getName(),
                source.getDisplayOrder(),
                source.getEstimatedMinutes(),
                source.getContent().length(),
                cleaned.substring(0, Math.min(PREVIEW_LENGTH, cleaned.length())),
                source.getIsPublished());
    }

    private GenerateQuestionsFromSourcesResponse.Source toContextSource(TheoryLesson source) {
        return new GenerateQuestionsFromSourcesResponse.Source(
                source.getId(), source.getTitle(), source.getTopic().getName(), source.getIsPublished());
    }

    private Long learningPathId(QuizLesson quiz) {
        return quiz.getTopic().getLearningPath().getId();
    }

    private String cleanContent(String content) {
        String withoutDangerousBlocks = SCRIPT_OR_STYLE.matcher(content == null ? "" : content).replaceAll(" ");
        String withoutHtml = HTML_TAG.matcher(withoutDangerousBlocks).replaceAll(" ");
        return WHITESPACE.matcher(withoutHtml).replaceAll(" ").trim();
    }

    private String stripJsonFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private String providerName(GenerateQuestionsFromSourcesRequest request) {
        return request.provider() == null ? null : request.provider().name();
    }

    private String trimmedOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeForComparison(String value) {
        return WHITESPACE.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void invalidGeneratedQuestions() {
        throw new AppException(ErrorCode.INVALID_AI_GENERATED_QUESTIONS);
    }

    private AiLlmExecutor.ChatResponseWithTokens sumUsage(
            AiLlmExecutor.ChatResponseWithTokens first,
            AiLlmExecutor.ChatResponseWithTokens second) {
        return new AiLlmExecutor.ChatResponseWithTokens(
                second.responseText(),
                sumNullable(first.inputTokens(), second.inputTokens()),
                sumNullable(first.outputTokens(), second.outputTokens()));
    }

    private Integer sumNullable(Integer first, Integer second) {
        return first == null && second == null ? null : (first == null ? 0 : first) + (second == null ? 0 : second);
    }

    private record ContextBuildResult(String content, List<Long> truncatedSourceIds) {
    }

    private record GeneratedPayload(List<GenerateQuestionsFromSourcesResponse.DraftQuestion> questions) {
    }
}
