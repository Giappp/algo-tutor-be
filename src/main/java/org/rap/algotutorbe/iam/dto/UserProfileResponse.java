package org.rap.algotutorbe.iam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String avatarUrl
) {
}
