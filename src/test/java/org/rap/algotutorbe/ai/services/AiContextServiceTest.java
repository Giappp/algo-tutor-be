package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.ai.dto.AiChatRequest;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.TheoryLesson;
import org.rap.algotutorbe.learning.repositories.LessonRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiContextServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private AiContextService aiContextService;

    @Test
    void buildContext_shouldBuildTheoryLessonContext() {
        TheoryLesson theoryLesson = new TheoryLesson();
        theoryLesson.setTitle("Binary Search");
        theoryLesson.setDifficulty(Difficulty.EASY);
        theoryLesson.setContent("Binary search is a fast search algorithm...");

        when(lessonRepository.findBySlug("binary-search")).thenReturn(Optional.of(theoryLesson));

        AiChatRequest request = new AiChatRequest(
                null,
                null,
                "binary-search",
                "GEMINI",
                "EXPLAIN",
                "Tell me more",
                null,
                null,
                null,
                null,
                null
        );

        String context = aiContextService.buildContext(request);

        assertThat(context).contains("[LESSON_CONTEXT]");
        assertThat(context).contains("Tiêu đề: Binary Search");
        assertThat(context).contains("Độ khó: EASY");
        assertThat(context).contains("Nội dung:\nBinary search is a fast search algorithm...");
    }

    @Test
    void buildContext_shouldBuildCodingLessonContextAndFailedTests() {
        CodingLesson codingLesson = new CodingLesson();
        codingLesson.setTitle("Two Sum");
        codingLesson.setDifficulty(Difficulty.EASY);
        codingLesson.setStatement("Given an array of integers...");
        codingLesson.setConstraints(List.of("2 <= nums.length <= 10^4"));

        when(lessonRepository.findBySlug("two-sum")).thenReturn(Optional.of(codingLesson));

        AiChatRequest request = new AiChatRequest(
                null,
                null,
                "two-sum",
                "GEMINI",
                "HINT",
                null,
                "class Solution {}",
                "java",
                "WRONG_ANSWER",
                "ArrayOutOfBounds",
                List.of("Input: [3,2,4], Target: 6 | Got: [0,0]")
        );

        String context = aiContextService.buildContext(request);

        assertThat(context).contains("[LESSON_CONTEXT]");
        assertThat(context).contains("Tiêu đề: Two Sum");
        assertThat(context).contains("Đề bài:\nGiven an array of integers...");
        assertThat(context).contains("Ràng buộc:\n[2 <= nums.length <= 10^4]");
        assertThat(context).contains("[JUDGE_RESULT]\nWRONG_ANSWER");
        assertThat(context).contains("[ERROR_MESSAGE]\nArrayOutOfBounds");
        assertThat(context).contains("[FAILED_TEST_CASES]");
        assertThat(context).contains("Testcase 1: Input: [3,2,4], Target: 6 | Got: [0,0]");
    }
}
