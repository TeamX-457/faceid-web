package com.campus.security.faceid.config;

import com.campus.security.faceid.model.Role;
import com.campus.security.faceid.model.User;
import com.campus.security.faceid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds a default admin + uploader account on first startup (only when the users table is
 * empty) so the dashboard is usable without manual database setup. Passwords are configurable
 * via env vars so a real deployment isn't left with publicly-known credentials; the fallbacks
 * here only apply to a fresh local/dev database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:admin123}")
    private String seedAdminPassword;

    @Value("${app.seed.uploader-password:uploader123}")
    private String seedUploaderPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode(seedAdminPassword))
                .fullName("Default Administrator")
                .role(Role.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);

        User uploader = User.builder()
                .username("uploader")
                .passwordHash(passwordEncoder.encode(seedUploaderPassword))
                .fullName("Default Uploader")
                .role(Role.UPLOADER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(uploader);

        log.info("No users found - seeded default accounts (admin, uploader). Passwords from app.seed.* config.");
    }
}
