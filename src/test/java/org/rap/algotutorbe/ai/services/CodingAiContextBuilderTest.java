package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.models.Topic;
import org.rap.algotutorbe.learning.repositories.LessonRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodingAiContextBuilderTest {

    @Mock
    private LessonRepository lessonRepository;

    private CodingAiContextBuilder builder;
    private CodingLesson lesson;
    private TheoryLesson source;

    @BeforeEach
    void setUp() {
        builder = new CodingAiContextBuilder(lessonRepository);
        LearningPath path = new LearningPath();
        path.setId(10L);
        Topic topic = new Topic();
        topic.setId(20L);
        topic.setName("Hashing");
        topic.setLearningPath(path);
        lesson = new CodingLesson();
        lesson.setId(30L);
        lesson.setTopic(topic);
        source = new TheoryLesson();
        source.setId(40L);
        source.setTitle("Hash Maps");
        source.setDisplayOrder(1);
        source.setContent("<script>bad()</script> Hash maps store key value pairs.");
        source.setIsPublished(true);
        source.setTopic(topic);
    }

    @Test
    void getSources_shouldReturnCleanPreview() {
        when(lessonRepository.findById(30L)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findQuestionSourcesByLearningPathId(10L)).thenReturn(List.of(source));

        var response = builder.getSources(builder.getCodingLesson(30L));

        assertThat(response.getFirst().contentPreview()).isEqualTo("Hash maps store key value pairs.");
    }

    @Test
    void build_shouldRejectSourceOutsideLearningPath() {
        when(lessonRepository.findQuestionSourcesByLearningPathId(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> builder.build(lesson, List.of(40L)))
                .isInstanceOf(AppException.class)
                .extracting("error")
                .isEqualTo(ErrorCode.INVALID_AI_CODING_SOURCES);
    }
}
