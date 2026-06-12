package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.CodingLessonRequestDTO;
import org.rap.algotutorbe.learning.dto.QuizLessonRequestDTO;
import org.rap.algotutorbe.learning.enums.Difficulty;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.QuizLesson;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownLessonRequestBuilderTest {

    private final MarkdownLessonRequestBuilder builder = new MarkdownLessonRequestBuilder();

    @Test
    void build_shouldUseMarkdownAsCodingStatementAndPreserveConfiguration() {
        CodingLesson lesson = new CodingLesson();
        lesson.setTitle("Two Sum");
        lesson.setType(LessonType.CODING);
        lesson.setDifficulty(Difficulty.EASY);
        lesson.setDisplayOrder(2);
        lesson.setBaseTimeLimitMs(3000);
        lesson.setBaseMemoryLimitMb(512);
        lesson.setConstraints(List.of("2 <= nums.length"));
        lesson.setStarterCode(Map.of("java", "class Solution {}"));
        lesson.setHints(List.of("Use a map"));

        CodingLessonRequestDTO request = (CodingLessonRequestDTO) builder.build(lesson, "# Two Sum\n\nSolve it.");

        assertThat(request.getStatement()).isEqualTo("# Two Sum\n\nSolve it.");
        assertThat(request.getBaseTimeLimitMs()).isEqualTo(3000);
        assertThat(request.getStarterCode()).containsEntry("java", "class Solution {}");
        assertThat(request.getTitle()).isEqualTo("Two Sum");
    }

    @Test
    void build_shouldParseQuizMarkdownIntoQuestions() {
        QuizLesson lesson = new QuizLesson();
        lesson.setTitle("Array Quiz");
        lesson.setType(LessonType.QUIZ);
        lesson.setDifficulty(Difficulty.EASY);
        lesson.setDisplayOrder(3);
        lesson.setPassingScore(70);

        String markdown = """
                # Array Quiz

                ## Question 1
                What is random access complexity?
                - [x] O(1)
                - [ ] O(n)
                > Explanation: Arrays support direct indexing.

                ## Question 2
                Select valid array operations.
                - [x] Read by index
                - [x] Write by index
                - [ ] Access a missing index safely
                """;

        QuizLessonRequestDTO request = (QuizLessonRequestDTO) builder.build(lesson, markdown);

        assertThat(request.getQuestions()).hasSize(2);
        assertThat(request.getQuestions().getFirst().question()).isEqualTo("What is random access complexity?");
        assertThat(request.getQuestions().getFirst().choices()).hasSize(2);
        assertThat(request.getQuestions().getFirst().type().name()).isEqualTo("SINGLE_CHOICE");
        assertThat(request.getQuestions().get(1).type().name()).isEqualTo("MULTIPLE_CHOICE");
    }

    @Test
    void build_shouldRejectQuizMarkdownWithoutCorrectChoice() {
        QuizLesson lesson = new QuizLesson();
        lesson.setType(LessonType.QUIZ);

        assertThatThrownBy(() -> builder.build(lesson, """
                ## Question 1
                Invalid question
                - [ ] First
                - [ ] Second
                """))
                .isInstanceOf(AppException.class);
    }
}
