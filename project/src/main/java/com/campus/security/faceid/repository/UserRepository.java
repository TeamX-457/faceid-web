package com.campus.security.faceid.repository;

import com.campus.security.faceid.model.User;
import com.campus.security.faceid.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find all active users by role
     */
    List<User> findByRoleAndIsActive(Role role, Boolean isActive);

    /**
     * Check if a username already exists
     */
    boolean existsByUsername(String username);
}
