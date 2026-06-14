package org.rap.algotutorbe.ai.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.GenerateQuestionsFromSourcesRequest;
import org.rap.algotutorbe.ai.dto.GenerateQuestionsFromSourcesResponse;
import org.rap.algotutorbe.ai.enums.LLMProvider;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.QuestionType;
import org.rap.algotutorbe.learning.models.QuizLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LessonRepository;
import org.rap.algotutorbe.learning.repositories.QuizQuestionRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAiQuizQuestionServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private AiLlmExecutor aiLlmExecutor;

    private AdminAiQuizQuestionService service;
    private QuizLesson quiz;
    private TheoryLesson source;

    @BeforeEach
    void setUp() {
        service = new AdminAiQuizQuestionService(
                lessonRepository, quizQuestionRepository, aiLlmExecutor, new ObjectMapper());

        LearningPath learningPath = new LearningPath();
        learningPath.setId(10L);
        learningPath.setName("Algorithms");

        Topic topic = new Topic();
        topic.setId(20L);
        topic.setName("Searching");
        topic.setDisplayOrder(1);
        topic.setLearningPath(learningPath);

        quiz = new QuizLesson();
        quiz.setId(30L);
        quiz.setTitle("Searching Quiz");
        quiz.setType(LessonType.QUIZ);
        quiz.setTopic(topic);

        source = new TheoryLesson();
        source.setId(40L);
        source.setTitle("Binary Search");
        source.setType(LessonType.THEORY);
        source.setDisplayOrder(1);
        source.setContent("<script>ignore()</script> Binary search halves a sorted range.");
        source.setIsPublished(false);
        source.setTopic(topic);
    }

    @Test
    void getQuestionSources_shouldReturnLightweightMetadata() {
        when(lessonRepository.findById(30L)).thenReturn(Optional.of(quiz));
        when(lessonRepository.findQuestionSourcesByLearningPathId(10L)).thenReturn(List.of(source));

        var response = service.getQuestionSources(30L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().lessonId()).isEqualTo(40L);
        assertThat(response.getFirst().contentPreview()).isEqualTo("Binary search halves a sorted range.");
        assertThat(response.getFirst().contentCharacterCount()).isEqualTo(source.getContent().length());
        assertThat(response.getFirst().isPublished()).isFalse();
    }

    @Test
    void generateQuestions_shouldRepairInvalidOutputOnceAndReturnValidDraft() {
        stubQuizAndSource();
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndex(30L)).thenReturn(List.of());
        when(aiLlmExecutor.callWithFallback(eq("GEMINI"), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens("not json", 100, 10))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens(validJson(), 50, 30));

        GenerateQuestionsFromSourcesResponse response = service.generateQuestions(30L, request(List.of(40L)));

        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().getFirst().type()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(response.context().sources().getFirst().isPublished()).isFalse();
        assertThat(response.inputTokens()).isEqualTo(150);
        assertThat(response.outputTokens()).isEqualTo(40);
        verify(aiLlmExecutor, times(2)).callWithFallback(eq("GEMINI"), anyList(), eq(null));
    }

    @Test
    void generateQuestions_shouldAcceptRawQuestionArrayAndAssignOrderIndex() {
        stubQuizAndSource();
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndex(30L)).thenReturn(List.of());
        when(aiLlmExecutor.callWithFallback(eq("GEMINI"), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens(rawArrayJson(), 100, 30));

        GenerateQuestionsFromSourcesResponse response = service.generateQuestions(30L, request(List.of(40L)));

        assertThat(response.questions()).hasSize(1);
        assertThat(response.questions().getFirst().orderIndex()).isEqualTo(1);
        verify(aiLlmExecutor).callWithFallback(eq("GEMINI"), anyList(), eq(null));
    }

    @Test
    void generateQuestions_shouldRemoveExplanationsWhenNotRequestedWithoutRepair() {
        stubQuizAndSource();
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndex(30L)).thenReturn(List.of());
        when(aiLlmExecutor.callWithFallback(eq("GEMINI"), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens(rawArrayJson(), 100, 30));

        GenerateQuestionsFromSourcesResponse response =
                service.generateQuestions(30L, request(List.of(40L), false));

        assertThat(response.questions().getFirst().explanation()).isNull();
        assertThat(response.questions().getFirst().choices())
                .allSatisfy(choice -> assertThat(choice.explanation()).isNull());
        verify(aiLlmExecutor).callWithFallback(eq("GEMINI"), anyList(), eq(null));
    }

    @Test
    void generateQuestions_shouldRejectDuplicateSourceIdsBeforeCallingLlm() {
        when(lessonRepository.findById(30L)).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> service.generateQuestions(30L, request(List.of(40L, 40L))))
                .isInstanceOf(AppException.class)
                .extracting("error")
                .isEqualTo(ErrorCode.INVALID_AI_QUESTION_SOURCES);
    }

    @Test
    void generateQuestions_shouldRejectInvalidOutputAfterRepair() {
        stubQuizAndSource();
        when(quizQuestionRepository.findByQuizIdOrderByOrderIndex(30L)).thenReturn(List.of());
        when(aiLlmExecutor.callWithFallback(eq("GEMINI"), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens("{}", null, null));

        assertThatThrownBy(() -> service.generateQuestions(30L, request(List.of(40L))))
                .isInstanceOf(AppException.class)
                .extracting("error")
                .isEqualTo(ErrorCode.INVALID_AI_GENERATED_QUESTIONS);
        verify(aiLlmExecutor, times(2)).callWithFallback(eq("GEMINI"), anyList(), eq(null));
    }

    private void stubQuizAndSource() {
        when(lessonRepository.findById(30L)).thenReturn(Optional.of(quiz));
        when(lessonRepository.findQuestionSourcesByLearningPathId(10L)).thenReturn(List.of(source));
    }

    private GenerateQuestionsFromSourcesRequest request(List<Long> sourceIds) {
        return request(sourceIds, true);
    }

    private GenerateQuestionsFromSourcesRequest request(List<Long> sourceIds, boolean includeExplanations) {
        return new GenerateQuestionsFromSourcesRequest(
                sourceIds,
                "Focus on concepts",
                LLMProvider.GEMINI,
                Difficulty.MEDIUM,
                Set.of(QuestionType.SINGLE_CHOICE),
                1,
                2,
                includeExplanations);
    }

    private String rawArrayJson() {
        return """
                [{
                  "question": "Why must binary search use sorted input?",
                  "type": "SINGLE_CHOICE",
                  "points": 2,
                  "explanation": "Ordering makes discarding a half valid.",
                  "choices": [
                    {"text": "To decide which half can be discarded", "isCorrect": true, "explanation": "Correct"},
                    {"text": "To allocate less memory", "isCorrect": false, "explanation": "Incorrect"}
                  ]
                }]
                """;
    }

    private String validJson() {
        return """
                {
                  "questions": [{
                    "question": "Why must binary search use sorted input?",
                    "type": "SINGLE_CHOICE",
                    "points": 2,
                    "orderIndex": 1,
                    "explanation": "Ordering makes discarding a half valid.",
                    "choices": [
                      {"text": "To decide which half can be discarded", "isCorrect": true, "explanation": "Correct"},
                      {"text": "To allocate less memory", "isCorrect": false, "explanation": null}
                    ]
                  }]
                }
                """;
    }
}
