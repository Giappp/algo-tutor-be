package org.rap.algotutorbe.ai.dto;

import java.util.UUID;

public record SubmissionToolResult(UUID id, String verdict,
                                   String language, String sourceCode) {
}
