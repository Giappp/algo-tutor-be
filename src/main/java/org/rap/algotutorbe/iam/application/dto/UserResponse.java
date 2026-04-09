package org.rap.algotutorbe.iam.application.dto;

public record UserResponse(Long id, String userName, String email, Integer totalSolved) {
}
