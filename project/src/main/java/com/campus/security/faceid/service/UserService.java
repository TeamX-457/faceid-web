package com.campus.security.faceid.service;

import com.campus.security.faceid.model.User;
import com.campus.security.faceid.model.Role;
import com.campus.security.faceid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for user management (CRUD operations, password hashing, authentication).
 * Passwords are always hashed with BCrypt and never stored in plain text.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * Register a new user with hashed password
     */
    public User registerUser(String username, String plainPassword, String fullName, Role role) {
        // Validate inputs
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        // Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(plainPassword);

        User user = User.builder()
                .username(username)
                .passwordHash(hashedPassword)
                .fullName(fullName)
                .role(role)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} with role {}", username, role);

        // Audit log: User creation
        auditLogService.logAction(null, "UNKNOWN", Role.ADMIN, "USER_CREATED",
                "USER", savedUser.getId(), null, null, null);

        return savedUser;
    }

    /**
     * Authenticate user by username and plain password
     * Returns the User if credentials are valid, Optional.empty() otherwise
     */
    public Optional<User> authenticateUser(String username, String plainPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: user not found: {}", username);
            return Optional.empty();
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.getIsActive()) {
            log.warn("Authentication failed: user inactive: {}", username);
            return Optional.empty();
        }

        // Compare plain password with hashed password
        if (passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
            log.info("User authenticated successfully: {}", username);
            return Optional.of(user);
        }

        log.warn("Authentication failed: invalid password for user: {}", username);
        return Optional.empty();
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get all active users
     */
    public List<User> getAllActiveUsers() {
        return userRepository.findAll().stream()
                .filter(User::getIsActive)
                .toList();
    }

    /**
     * Get all users with a specific role
     */
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRoleAndIsActive(role, true);
    }

    /**
     * Update user details
     */
    public User updateUser(Long userId, String fullName, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setFullName(fullName);
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);
        log.info("User updated: {} (ID: {})", user.getUsername(), userId);

        // Audit log: User update
        auditLogService.logAction(null, "UNKNOWN", Role.ADMIN, "USER_UPDATED",
                "USER", userId, null, null, null);

        return updated;
    }

    /**
     * Deactivate a user (soft delete)
     */
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User deactivated: {} (ID: {})", user.getUsername(), userId);

        // Audit log: User deletion
        auditLogService.logAction(null, "UNKNOWN", Role.ADMIN, "USER_DELETED",
                "USER", userId, null, null, null);
    }

    /**
     * Delete user permanently
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        userRepository.delete(user);
        log.info("User deleted permanently: {} (ID: {})", user.getUsername(), userId);

        // Audit log: User deletion
        auditLogService.logAction(null, "UNKNOWN", Role.ADMIN, "USER_DELETED",
                "USER", userId, null, null, null);
    }
}
