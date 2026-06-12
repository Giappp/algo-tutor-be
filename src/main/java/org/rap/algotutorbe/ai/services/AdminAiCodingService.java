package org.rap.algotutorbe.ai.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.ai.dto.AiQuestionSourceResponse;
import org.rap.algotutorbe.ai.dto.CodingAiGenerationResponse;
import org.rap.algotutorbe.ai.dto.GenerateCodingEditorialRequest;
import org.rap.algotutorbe.ai.dto.GenerateCodingProblemRequest;
import org.rap.algotutorbe.ai.dto.GenerateStarterCodeRequest;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAiCodingService {

    private final CodingAiContextBuilder contextBuilder;
    private final CodingDraftValidator draftValidator;
    private final CodingCompileValidator compileValidator;
    private final AiLlmExecutor aiLlmExecutor;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<AiQuestionSourceResponse> getSources(Long lessonId) {
        return contextBuilder.getSources(contextBuilder.getCodingLesson(lessonId));
    }

    @Transactional(readOnly = true)
    public CodingAiGenerationResponse<CodingAiGenerationResponse.ProblemContent> generateProblem(
            Long lessonId,
            GenerateCodingProblemRequest request) {
        CodingLesson lesson = contextBuilder.getCodingLesson(lessonId);
        CodingAiContextBuilder.BuiltContext context = contextBuilder.build(lesson, request.sourceLessonIds());
        String instruction = """
                Generate problem content only. Return JSON:
                {"statement":"...","constraints":["..."],"examples":[{"input":"...","output":"...",
                "explanation":"...","imageUrl":null}],"hints":["..."]}
                Use exactly %d examples and %d hints. Do not include starter code, editorial, solution, or test cases.
                """.formatted(request.exampleCount(), request.hintCount());
        var generated = generate(
                lesson,
                context,
                providerName(request.provider()),
                request.prompt(),
                "problem",
                instruction + "\nDifficulty: " + request.difficulty(),
                CodingAiGenerationResponse.ProblemContent.class,
                content -> draftValidator.validateProblem(content, request.exampleCount(), request.hintCount()));
        return response(lessonId, generated.content(), context, generated.tokens());
    }

    @Transactional(readOnly = true)
    public CodingAiGenerationResponse<CodingAiGenerationResponse.EditorialContent> generateEditorial(
            Long lessonId,
            GenerateCodingEditorialRequest request) {
        CodingLesson lesson = contextBuilder.getCodingLesson(lessonId);
        CodingAiContextBuilder.BuiltContext context = contextBuilder.build(lesson, request.sourceLessonIds());
        String instruction = """
                Generate a reference editorial. Return JSON:
                {"language":"%s","sourceCode":"...","approachSummary":"...",
                "timeComplexity":"O(...)","spaceComplexity":"O(...)"}
                The source code must compile as a standalone %s source file. Do not claim it passed judge test cases.
                """.formatted(request.language(), request.language().getFileName());
        var generated = generate(
                lesson,
                context,
                providerName(request.provider()),
                request.prompt(),
                "editorial",
                instruction,
                CodingAiGenerationResponse.EditorialContent.class,
                content -> {
                    draftValidator.validateEditorial(content, request.language());
                    compileValidator.validate(content.language(), content.sourceCode());
                });
        return response(lessonId, generated.content(), context, generated.tokens());
    }

    @Transactional(readOnly = true)
    public CodingAiGenerationResponse<CodingAiGenerationResponse.StarterCodeContent> generateStarterCode(
            Long lessonId,
            GenerateStarterCodeRequest request) {
        Set<ProgrammingLanguage> languages = new LinkedHashSet<>(request.languages());
        if (languages.size() != request.languages().size()) {
            throw new AppException(ErrorCode.INVALID_PAYLOAD);
        }
        CodingLesson lesson = contextBuilder.getCodingLesson(lessonId);
        CodingAiContextBuilder.BuiltContext context = contextBuilder.build(lesson, request.sourceLessonIds());
        String instruction = """
                Generate starter scaffold only for languages %s. Return JSON:
                {"starterCode":{"java":"...","python":"...","cpp":"..."},
                "signatureSummary":"Function: solve(...). ..."}
                Include exactly the requested lowercase language keys. Each scaffold must compile or parse,
                contain the same function name and compatible parameter/return types, and contain no solution.
                Start every scaffold with a language comment in this exact normalized contract form:
                Signature: solve(parameterName:type, ...) -> returnType
                """.formatted(languages);
        var generated = generate(
                lesson,
                context,
                providerName(request.provider()),
                request.prompt(),
                "starter-code",
                instruction,
                CodingAiGenerationResponse.StarterCodeContent.class,
                content -> {
                    draftValidator.validateStarter(content, languages);
                    compileValidator.validateAll(content.starterCode());
                });
        return response(lessonId, generated.content(), context, generated.tokens());
    }

    private <T> Generated<T> generate(
            CodingLesson lesson,
            CodingAiContextBuilder.BuiltContext context,
            String provider,
            String adminPrompt,
            String assetType,
            String instruction,
            Class<T> responseType,
            Consumer<T> validator) {
        List<Message> messages = messages(lesson, context.sourceContent(), adminPrompt, instruction);
        AiLlmExecutor.ChatResponseWithTokens first = aiLlmExecutor.callWithFallback(provider, messages, null);
        try {
            T content = parseAndValidate(first.responseText(), responseType, validator);
            return new Generated<>(content, first);
        } catch (AppException invalidFirstOutput) {
            if (invalidFirstOutput.getError() != ErrorCode.INVALID_AI_CODING_DRAFT
                    && invalidFirstOutput.getError() != ErrorCode.AI_GENERATED_CODE_COMPILE_FAILED) {
                throw invalidFirstOutput;
            }
            log.warn("Invalid generated coding {} for lesson [{}]; attempting one repair", assetType, lesson.getId());
            AiLlmExecutor.ChatResponseWithTokens repaired = aiLlmExecutor.callWithFallback(
                    provider, repairMessages(first.responseText(), instruction), null);
            T content = parseAndValidate(repaired.responseText(), responseType, validator);
            return new Generated<>(content, sumUsage(first, repaired));
        }
    }

    private List<Message> messages(
            CodingLesson lesson,
            String sourceContent,
            String adminPrompt,
            String instruction) {
        return List.of(
                new SystemMessage("""
                        You generate grounded draft assets for an admin coding lesson studio.
                        Return only valid raw JSON. Do not use Markdown fences or include commentary.
                        Never generate or modify test cases. Use source lessons only as supporting knowledge.
                        """),
                new UserMessage("""
                        Learning path: %s
                        Topic: %s
                        Coding lesson title: %s
                        Current statement: %s
                        Current constraints: %s
                        Current examples: %s
                        Current starter code: %s
                        Admin instruction: %s

                        %s

                        %s
                        """.formatted(
                        lesson.getTopic().getLearningPath().getName(),
                        lesson.getTopic().getName(),
                        lesson.getTitle(),
                        valueOrEmpty(lesson.getStatement()),
                        lesson.getConstraints(),
                        lesson.getExamples(),
                        lesson.getStarterCode(),
                        valueOrEmpty(adminPrompt).trim(),
                        instruction,
                        sourceContent)));
    }

    private List<Message> repairMessages(String invalidOutput, String instruction) {
        return List.of(
                new SystemMessage("Repair the draft. Return only valid raw JSON and preserve its intended meaning."),
                new UserMessage(instruction + "\nInvalid draft:\n" + invalidOutput));
    }

    private <T> T parseAndValidate(String raw, Class<T> type, Consumer<T> validator) {
        try {
            T content = objectMapper.readValue(stripJsonFence(raw), type);
            validator.accept(content);
            return content;
        } catch (JsonProcessingException | RuntimeException e) {
            if (e instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.INVALID_AI_CODING_DRAFT, e);
        }
    }

    private <T> CodingAiGenerationResponse<T> response(
            Long lessonId,
            T content,
            CodingAiContextBuilder.BuiltContext context,
            AiLlmExecutor.ChatResponseWithTokens tokens) {
        return new CodingAiGenerationResponse<>(
                lessonId, content, context.responseContext(), tokens.inputTokens(), tokens.outputTokens());
    }

    private String stripJsonFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private String providerName(Object provider) {
        return provider == null ? null : provider.toString();
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private AiLlmExecutor.ChatResponseWithTokens sumUsage(
            AiLlmExecutor.ChatResponseWithTokens first,
            AiLlmExecutor.ChatResponseWithTokens second) {
        return new AiLlmExecutor.ChatResponseWithTokens(
                second.responseText(),
                sum(first.inputTokens(), second.inputTokens()),
                sum(first.outputTokens(), second.outputTokens()));
    }

    private Integer sum(Integer first, Integer second) {
        return first == null && second == null ? null : (first == null ? 0 : first) + (second == null ? 0 : second);
    }

    private record Generated<T>(T content, AiLlmExecutor.ChatResponseWithTokens tokens) {
    }
}
