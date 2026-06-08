package org.rap.algotutorbe.ai.dto;

import java.util.List;

public record ProblemToolResult(Long id, String title, String difficulty,
                                String statement, List<String> constraints) {
}
