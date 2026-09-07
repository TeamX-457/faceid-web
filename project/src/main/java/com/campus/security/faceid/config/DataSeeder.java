package com.campus.security.faceid.config;

import com.campus.security.faceid.model.Role;
import com.campus.security.faceid.model.User;
import com.campus.security.faceid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds a default admin account on first startup so the dashboard is usable
 * without manual database setup. Username: admin / Password: admin123.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Default Administrator")
                .role(Role.ADMIN)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);

        User uploader = User.builder()
                .username("uploader")
                .passwordHash(passwordEncoder.encode("uploader123"))
                .fullName("Default Uploader")
                .role(Role.UPLOADER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(uploader);

        log.info("No users found - seeded default accounts (admin/admin123, uploader/uploader123)");
    }
}
