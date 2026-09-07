package com.campus.security.faceid.dto;

import com.campus.security.faceid.model.Role;
import lombok.*;

/**
 * Request DTO for /auth/login endpoint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {
    private String username;
    private String password;
}
