package com.campus.security.faceid.dto;

import com.campus.security.faceid.model.Role;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

/**
 * Response DTO for /auth/login endpoint. Snake-cased so full_name matches what the Android
 * app's LoginResponse model requires; the other extra fields (user_id, token_type, expires_in)
 * are ignored by the mobile client but kept for the admin web dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LoginResponseDTO {
    private String token;
    private String username;
    private String fullName;
    private Role role;
    private Long userId;
    private String tokenType = "Bearer";
    private long expiresIn;  // Expiration time in seconds (8 hours = 28800 seconds)
}
