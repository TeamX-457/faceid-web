package com.campus.security.faceid.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /auth/signup. Matches the Android app's SignUpRequest exactly
 * (username, password, email, full_name) - snake_case since the Android client sends full_name.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SignUpRequestDTO {
    private String username;
    private String password;
    private String email;
    private String fullName;
}
