package com.campus.security.faceid.controller;

import com.campus.security.faceid.dto.LoginRequestDTO;
import com.campus.security.faceid.dto.LoginResponseDTO;
import com.campus.security.faceid.dto.SignUpRequestDTO;
import com.campus.security.faceid.model.Role;
import com.campus.security.faceid.model.User;
import com.campus.security.faceid.security.JwtTokenProvider;
import com.campus.security.faceid.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Authentication Controller for login and user management endpoints.
 * 
 * Public endpoints:
 * - POST /auth/login - Login with username/password, returns JWT token
 * 
 * Admin-only endpoints:
 * - GET /users - List all users
 * - POST /users - Create new user (UPLOADER or ADMIN)
 * - DELETE /users/{id} - Deactivate/delete user
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    private static final long JWT_EXPIRY_SECONDS = 28800;  // 8 hours

    /**
     * Login with username and password
     * Returns JWT token for subsequent requests
     * Public endpoint - no authentication required
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            // Validate input
            if (loginRequest.getUsername() == null || loginRequest.getUsername().isBlank() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("Username and password are required"));
            }

            // Authenticate user
            Optional<User> userOpt = userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());

            if (userOpt.isEmpty()) {
                log.warn("Login failed: Invalid credentials for username: {}", loginRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("Invalid username or password"));
            }

            User user = userOpt.get();

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

            // Build response
            LoginResponseDTO response = LoginResponseDTO.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .userId(user.getId())
                    .expiresIn(JWT_EXPIRY_SECONDS)
                    .build();

            log.info("User logged in successfully: {}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Login failed: " + e.getMessage()));
        }
    }

    /**
     * Self-service sign-up for mobile uploaders. Public endpoint - no authentication required.
     * Always creates a Role.UPLOADER account (never ADMIN) and auto-logs in, matching the shape
     * of /auth/login exactly so the Android app's post-signup flow is identical to post-login.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequestDTO request) {
        try {
            if (request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank() ||
                request.getFullName() == null || request.getFullName().isBlank() ||
                request.getEmail() == null || request.getEmail().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("username, password, email, and full_name are all required"));
            }

            User user;
            try {
                user = userService.registerUploaderSelfSignup(
                        request.getUsername(), request.getPassword(), request.getFullName(), request.getEmail());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorMessage(e.getMessage()));
            }

            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

            LoginResponseDTO response = LoginResponseDTO.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .userId(user.getId())
                    .expiresIn(JWT_EXPIRY_SECONDS)
                    .build();

            log.info("New uploader signed up and auto-logged in: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Signup error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Signup failed: " + e.getMessage()));
        }
    }

    /**
     * Get all users - ADMIN ONLY
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllActiveUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Create a new user - ADMIN ONLY
     * Request body: {username, password, full_name, role}
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            // Validate input
            if (request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank() ||
                request.getFullName() == null || request.getFullName().isBlank() ||
                request.getRole() == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("All fields are required: username, password, full_name, role"));
            }

            // Create user
            User newUser = userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getFullName(),
                    request.getRole()
            );

            log.info("New user created by admin: {}", newUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);

        } catch (IllegalArgumentException e) {
            log.warn("Error creating user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Get a specific user by ID - ADMIN ONLY
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user.get());
    }

    /**
     * Delete/deactivate a user - ADMIN ONLY
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deactivateUser(id);
            log.info("User deleted/deactivated by admin: ID {}", id);
            return ResponseEntity.ok(new SuccessMessage("User deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to delete user: " + e.getMessage()));
        }
    }

    /**
     * Get current logged-in user info
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof String) {
                String username = (String) authentication.getPrincipal();
                Optional<User> user = userService.getUserByUsername(username);
                if (user.isPresent()) {
                    return ResponseEntity.ok(user.get());
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorMessage("User not found"));
        } catch (Exception e) {
            log.error("Error retrieving current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to retrieve user info"));
        }
    }

    /**
     * Request DTO for user creation
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String fullName;
        private Role role;
    }

    /**
     * Generic error message response
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ErrorMessage {
        private String message;
    }

    /**
     * Generic success message response
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class SuccessMessage {
        private String message;
    }
}
