package com.campus.security.faceid.config;

import com.campus.security.faceid.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security Configuration for FaceID Campus Backend.
 *
 * JWT tokens via "Authorization: Bearer <token>" header are the sole auth mechanism.
 * Uses deny-by-default strategy: all endpoints require authentication/authorization.
 * Role-based access control via @PreAuthorize("hasRole('ADMIN')") / @PreAuthorize("hasRole('UPLOADER')")
 * Public endpoints (e.g., /auth/login) are explicitly allowed.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true,  // Enable @PreAuthorize and @PostAuthorize
        securedEnabled = true,   // Enable @Secured
        jsr250Enabled = true     // Enable @RolesAllowed
)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Wide-open in local dev (any localhost port); locked to the deployed frontend's
        // origin otherwise. The Android app doesn't send an Origin header at all, so it's
        // unaffected by this either way.
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://faceid-frontend.onrender.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/auth/login", "/auth/signup").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // All other endpoints require authentication (deny-by-default)
                .anyRequest().authenticated()
            )
            // Without this, Spring Security's default fallback for a missing/invalid/expired
            // token is a bare 403 - indistinguishable from a real "wrong role" denial, and the
            // frontend's stale-session recovery (clear + redirect to login) only triggers on
            // 401. This makes "not authenticated" and "authenticated but not permitted" two
            // genuinely different, correctly-coded responses.
            .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("timestamp", LocalDateTime.now());
                body.put("status", 401);
                body.put("error", "Unauthorized");
                body.put("message", "Authentication failed: missing, invalid, or expired token. Please login again.");
                body.put("path", request.getRequestURI());
                objectMapper.writeValue(response.getWriter(), body);
            }))
            // Add JWT filter BEFORE UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}