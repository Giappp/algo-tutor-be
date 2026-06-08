package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.enums.AiChatMode;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponseGuardrailServiceTest {

    private final AiResponseGuardrailService guardrailService = new AiResponseGuardrailService();

    @Test
    void enforceLessonDisclosurePolicy_shouldAllowSolutionFullCode() {
        String response = """
                ```java
                class Solution {
                    public int solve() { return 1; }
                }
                ```
                """;

        String result = guardrailService.enforceLessonDisclosurePolicy(response, AiChatMode.SOLUTION, false);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void enforceLessonDisclosurePolicy_shouldReplaceHintCodeBlock() {
        String response = """
                Đây là code:
                ```java
                return nums[0];
                ```
                """;

        String result = guardrailService.enforceLessonDisclosurePolicy(response, AiChatMode.HINT, false);

        assertThat(result).doesNotContain("return nums[0]");
        assertThat(result).contains("gợi ý từng bước");
    }

    @Test
    void enforceLessonDisclosurePolicy_shouldReplaceFullCodeOutsideSolution() {
        String response = """
                ```java
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        for (int i = 0; i < nums.length; i++) {
                            for (int j = i + 1; j < nums.length; j++) {
                                if (nums[i] + nums[j] == target) return new int[]{i, j};
                            }
                        }
                        return new int[0];
                    }
                }
                ```
                """;

        String result = guardrailService.enforceLessonDisclosurePolicy(response, AiChatMode.EXPLAIN, false);

        assertThat(result).doesNotContain("class Solution");
        assertThat(result).contains("SOLUTION");
    }

    @Test
    void shouldBufferStreamingResponse_shouldOnlySkipSolution() {
        assertThat(guardrailService.shouldBufferStreamingResponse(AiChatMode.HINT)).isTrue();
        assertThat(guardrailService.shouldBufferStreamingResponse(AiChatMode.SOLUTION)).isFalse();
    }
}
