package org.rap.algotutorbe.judge.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PistonRequestTest {

    @Test
    void serializesPistonMemoryLimitUsingBytesContract() throws Exception {
        PistonRequest request = new PistonRequest(
                "python",
                "*",
                List.of(new PistonFile("main.py", "print(1)")),
                "",
                2_000,
                10_000,
                256 * 1024 * 1024
        );

        String json = new ObjectMapper().writeValueAsString(request);

        assertThat(json).contains("\"run_memory_limit\":268435456");
        assertThat(json).doesNotContain("memory_limit_mb");
    }
}
