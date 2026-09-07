package com.campus.security.faceid.dto;

import com.campus.security.faceid.model.Role;
import lombok.*;

/**
 * Response DTO for /auth/login endpoint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {
    private String token;
    private String username;
    private String fullName;
    private Role role;
    private Long userId;
    private String tokenType = "Bearer";
    private long expiresIn;  // Expiration time in seconds (8 hours = 28800 seconds)
}
