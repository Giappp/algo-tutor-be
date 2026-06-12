package org.rap.algotutorbe.ai.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.CodingAiGenerationResponse;
import org.rap.algotutorbe.ai.dto.GenerateCodingEditorialRequest;
import org.rap.algotutorbe.ai.dto.GenerateCodingProblemRequest;
import org.rap.algotutorbe.ai.dto.GenerateStarterCodeRequest;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Topic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAiCodingServiceTest {

    @Mock
    private CodingAiContextBuilder contextBuilder;
    @Mock
    private CodingCompileValidator compileValidator;
    @Mock
    private AiLlmExecutor aiLlmExecutor;

    private AdminAiCodingService service;
    private CodingLesson lesson;
    private CodingAiContextBuilder.BuiltContext context;

    @BeforeEach
    void setUp() {
        service = new AdminAiCodingService(
                contextBuilder, new CodingDraftValidator(), compileValidator, aiLlmExecutor, new ObjectMapper());

        LearningPath path = new LearningPath();
        path.setName("Algorithms");
        Topic topic = new Topic();
        topic.setName("Arrays");
        topic.setLearningPath(path);
        lesson = new CodingLesson();
        lesson.setId(30L);
        lesson.setTitle("Pair Sum");
        lesson.setStatement("Find a pair");
        lesson.setTopic(topic);
        context = new CodingAiContextBuilder.BuiltContext(
                "[SOURCE]arrays[/SOURCE]",
                new CodingAiGenerationResponse.GenerationContext(List.of(), List.of()));
    }

    @Test
    void generateProblem_shouldReturnValidatedDraft() {
        stubContext();
        when(aiLlmExecutor.callWithFallback(eq("GEMINI"), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens(problemJson(), 100, 40));

        var response = service.generateProblem(30L, new GenerateCodingProblemRequest(
                List.of(), LLMProvider.GEMINI, "Beginner", Difficulty.EASY, 1, 1));

        assertThat(response.content().statement()).contains("Pair Sum");
        assertThat(response.content().examples()).hasSize(1);
        assertThat(response.content().hints()).containsExactly("Use a map.");
        assertThat(response.inputTokens()).isEqualTo(100);
    }

    @Test
    void generateEditorial_shouldCompileBeforeReturning() {
        stubContext();
        when(aiLlmExecutor.callWithFallback(eq("OPENAI"), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens(editorialJson(), 100, 50));

        var response = service.generateEditorial(30L, new GenerateCodingEditorialRequest(
                List.of(), LLMProvider.OPENAI, "Readable", ProgrammingLanguage.JAVA));

        assertThat(response.content().timeComplexity()).isEqualTo("O(n)");
        verify(compileValidator).validate(ProgrammingLanguage.JAVA, "class Solution {}");
    }

    @Test
    void generateStarterCode_shouldRepairInvalidDraftOnce() {
        stubContext();
        when(aiLlmExecutor.callWithFallback(eq(null), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens("{}", 20, 5))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens(starterJson(), 30, 10));

        var response = service.generateStarterCode(30L, new GenerateStarterCodeRequest(
                List.of(), null, "Expose solve", List.of(ProgrammingLanguage.JAVA, ProgrammingLanguage.PYTHON)));

        assertThat(response.content().starterCode()).containsOnlyKeys("java", "python");
        assertThat(response.inputTokens()).isEqualTo(50);
        verify(compileValidator).validateAll(response.content().starterCode());
        verify(aiLlmExecutor, times(2)).callWithFallback(eq(null), anyList(), eq(null));
    }

    @Test
    void generateProblem_shouldNotRepairProviderFailure() {
        stubContext();
        when(aiLlmExecutor.callWithFallback(eq("GEMINI"), anyList(), eq(null)))
                .thenThrow(new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> service.generateProblem(30L, new GenerateCodingProblemRequest(
                List.of(), LLMProvider.GEMINI, "Beginner", Difficulty.EASY, 1, 1)))
                .isInstanceOf(AppException.class)
                .extracting("error")
                .isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
        verify(aiLlmExecutor).callWithFallback(eq("GEMINI"), anyList(), eq(null));
    }

    @Test
    void generateStarterCode_shouldRejectDuplicateLanguagesBeforeCallingLlm() {
        assertThatThrownBy(() -> service.generateStarterCode(30L, new GenerateStarterCodeRequest(
                List.of(), null, null, List.of(ProgrammingLanguage.JAVA, ProgrammingLanguage.JAVA))))
                .isInstanceOf(AppException.class)
                .extracting("error")
                .isEqualTo(ErrorCode.INVALID_PAYLOAD);
    }

    private void stubContext() {
        when(contextBuilder.getCodingLesson(30L)).thenReturn(lesson);
        when(contextBuilder.build(eq(lesson), eq(List.of()))).thenReturn(context);
    }

    private String problemJson() {
        return """
                {
                  "statement":"# Pair Sum",
                  "constraints":["2 <= nums.length"],
                  "examples":[{"input":"[2,7], 9","output":"[0,1]","explanation":"Pair found","imageUrl":null}],
                  "hints":["Use a map."]
                }
                """;
    }

    private String editorialJson() {
        return """
                {
                  "language":"JAVA",
                  "sourceCode":"class Solution {}",
                  "approachSummary":"Use a map.",
                  "timeComplexity":"O(n)",
                  "spaceComplexity":"O(n)"
                }
                """;
    }

    private String starterJson() {
        return """
                {
                  "starterCode":{
                    "java":"// Signature: solve() -> void\\nclass Solution { void solve() { throw new UnsupportedOperationException(); } }",
                    "python":"# Signature: solve() -> void\\nclass Solution:\\n    def solve(self):\\n        pass"
                  },
                  "signatureSummary":"Function: solve(). No parameters."
                }
                """;
    }
}
