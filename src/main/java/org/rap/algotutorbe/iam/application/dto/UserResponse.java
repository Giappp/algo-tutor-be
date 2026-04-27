package org.rap.algotutorbe.iam.application.dto;

import java.util.UUID;

public record UserResponse(UUID id, String userName, String email, Integer totalSolved) {
}
