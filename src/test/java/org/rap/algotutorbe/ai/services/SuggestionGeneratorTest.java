package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.enums.AiChatMode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SuggestionGeneratorTest {

    private static final Set<String> VALID_MODES = Set.of(
            "HINT", "EXPLAIN", "DEBUG", "REVIEW", "COMPLEXITY", "SOLUTION", "NEXT_STEP"
    );
    private SuggestionGenerator suggestionGenerator;

    @BeforeEach
    void setUp() {
        suggestionGenerator = new SuggestionGenerator();
    }

    @Test
    void generate_hintMode_codingLesson_returns4Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.HINT, "Here is a hint...", "CodingLesson");

        assertThat(suggestions).hasSize(4);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_hintMode_theoryLesson_returns3Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.HINT, "Here is a hint...", "TheoryLesson");

        assertThat(suggestions).hasSize(3);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_debugMode_returns4Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.DEBUG, "Found a bug...", "CodingLesson");

        assertThat(suggestions).hasSize(4);
        assertValidSuggestions(suggestions);

        // Verify contextual suggestions for DEBUG mode
        Set<String> modes = suggestions.stream()
                .map(AiSuggestion::mode)
                .collect(Collectors.toSet());
        assertThat(modes).contains("REVIEW", "COMPLEXITY", "HINT");
    }

    @Test
    void generate_explainMode_codingLesson_returns4Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.EXPLAIN, "The algorithm works by...", "CodingLesson");

        assertThat(suggestions).hasSize(4);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_explainMode_theoryLesson_returns3Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.EXPLAIN, "The concept is...", "TheoryLesson");

        assertThat(suggestions).hasSize(3);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_reviewMode_returns3Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.REVIEW, "Your code looks good...", "CodingLesson");

        assertThat(suggestions).hasSize(3);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_complexityMode_returns3Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.COMPLEXITY, "O(n log n)...", "CodingLesson");

        assertThat(suggestions).hasSize(3);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_solutionMode_codingLesson_returns3Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.SOLUTION, "Here is the solution...", "CodingLesson");

        assertThat(suggestions).hasSize(3);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_solutionMode_theoryLesson_returns2Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.SOLUTION, "Here is the solution...", "TheoryLesson");

        assertThat(suggestions).hasSize(2);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_nextStepMode_returns3Suggestions() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.NEXT_STEP, "Next you should...", "CodingLesson");

        assertThat(suggestions).hasSize(3);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_nullLessonType_doesNotFail() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.HINT, "A hint...", null);

        assertThat(suggestions).hasSizeBetween(2, 4);
        assertValidSuggestions(suggestions);
    }

    @Test
    void generate_nullAiResponse_doesNotFail() {
        List<AiSuggestion> suggestions = suggestionGenerator.generate(
                AiChatMode.DEBUG, null, "CodingLesson");

        assertThat(suggestions).hasSizeBetween(2, 4);
        assertValidSuggestions(suggestions);
    }

    @Test
    void canAskNextHint_hintsRemaining_returnsTrue() {
        assertThat(suggestionGenerator.canAskNextHint(0, 3)).isTrue();
        assertThat(suggestionGenerator.canAskNextHint(1, 3)).isTrue();
        assertThat(suggestionGenerator.canAskNextHint(2, 3)).isTrue();
    }

    @Test
    void canAskNextHint_noHintsRemaining_returnsFalse() {
        assertThat(suggestionGenerator.canAskNextHint(3, 3)).isFalse();
        assertThat(suggestionGenerator.canAskNextHint(4, 3)).isFalse();
    }

    @Test
    void canAskNextHint_zeroMaxHints_returnsFalse() {
        assertThat(suggestionGenerator.canAskNextHint(0, 0)).isFalse();
    }

    /**
     * Validates that all suggestions meet the structural requirements:
     * - label is not null and ≤ 100 chars
     * - mode is a valid AiChatMode value
     * - message is not null and ≤ 500 chars
     */
    private void assertValidSuggestions(List<AiSuggestion> suggestions) {
        for (AiSuggestion suggestion : suggestions) {
            assertThat(suggestion.label())
                    .isNotNull()
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(100);
            assertThat(suggestion.mode())
                    .isNotNull()
                    .isIn(VALID_MODES);
            assertThat(suggestion.message())
                    .isNotNull()
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(500);
        }
    }
}
