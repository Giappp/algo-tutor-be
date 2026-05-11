package org.rap.algotutorbe.iam.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private String avatar;
}
