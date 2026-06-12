package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AdminLessonContentGenerateRequest;
import org.rap.algotutorbe.ai.dto.AdminLessonContentGenerateResponse;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.enums.Level;
import org.rap.algotutorbe.learning.dto.TheoryLessonRequestDTO;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LessonRepository;

import java.util.LinkedHashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLessonContentGenerationServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private AiLlmExecutor aiLlmExecutor;

    private AdminLessonContentGenerationService service;
    private TheoryLesson lesson;

    @BeforeEach
    void setUp() {
        service = new AdminLessonContentGenerationService(
                lessonRepository,
                aiLlmExecutor,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new MarkdownLessonRequestBuilder());

        LearningPath learningPath = new LearningPath();
        learningPath.setId(10L);
        learningPath.setName("Data Structures");
        learningPath.setLevel(Level.BEGINNER);
        learningPath.setDescription("Learn fundamental data structures");
        learningPath.setGoal("Solve common interview problems");

        Topic topic = new Topic();
        topic.setId(20L);
        topic.setName("Arrays");
        topic.setDescription("Array fundamentals");
        topic.setLearningPath(learningPath);

        lesson = new TheoryLesson();
        lesson.setId(30L);
        lesson.setTitle("Array Basics");
        lesson.setType(LessonType.THEORY);
        lesson.setDifficulty(Difficulty.EASY);
        lesson.setDisplayOrder(1);
        lesson.setContent("Old content");
        lesson.setTopic(topic);
        topic.setLessons(new LinkedHashSet<>());
        topic.getLessons().add(lesson);
    }

    @Test
    void generate_shouldReturnStructuredDraftWithCurriculumContext() {
        when(lessonRepository.findById(30L)).thenReturn(Optional.of(lesson));
        when(aiLlmExecutor.callWithFallback(eq("GEMINI"), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens(
                        "# New content",
                        100,
                        50));

        AdminLessonContentGenerateResponse response = service.generate(
                30L,
                new AdminLessonContentGenerateRequest("GEMINI", "Rewrite for beginners"));

        assertThat(response.lessonId()).isEqualTo(30L);
        assertThat(response.lessonType()).isEqualTo(LessonType.THEORY);
        assertThat(response.content()).isInstanceOf(TheoryLessonRequestDTO.class);
        TheoryLessonRequestDTO content = (TheoryLessonRequestDTO) response.content();
        assertThat(content.getContent()).isEqualTo("# New content");
        assertThat(content.getTitle()).isEqualTo("Array Basics");
        assertThat(content.getType()).isEqualTo(LessonType.THEORY);
        assertThat(content.getDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(response.context().learningPathName()).isEqualTo("Data Structures");
        assertThat(response.context().topicName()).isEqualTo("Arrays");
        assertThat(response.inputTokens()).isEqualTo(100);
        assertThat(response.outputTokens()).isEqualTo(50);
    }

    @Test
    void generate_shouldRejectInvalidAiContent() {
        when(lessonRepository.findById(30L)).thenReturn(Optional.of(lesson));
        when(aiLlmExecutor.callWithFallback(eq(null), anyList(), eq(null)))
                .thenReturn(new AiLlmExecutor.ChatResponseWithTokens("  ", null, null));

        assertThatThrownBy(() -> service.generate(
                30L,
                new AdminLessonContentGenerateRequest(null, "Rewrite")))
                .isInstanceOf(AppException.class)
                .extracting("error")
                .isEqualTo(org.rap.algotutorbe.common.errors.ErrorCode.INVALID_AI_GENERATED_CONTENT);
    }
}
