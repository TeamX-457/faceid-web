package com.campus.security.faceid.security;

import com.campus.security.faceid.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Custom user details object stored in JWT authentication token for easy access to user information.
 */
@Getter
@AllArgsConstructor
public class JwtUserDetails {
    private Long userId;
    private String username;
    private Role role;
}
