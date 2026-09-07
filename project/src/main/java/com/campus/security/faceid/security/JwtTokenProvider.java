package com.campus.security.faceid.security;

import com.campus.security.faceid.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider for generating, validating, and extracting claims from JWT tokens.
 * Uses JJWT library with HS256 (HMAC SHA-256) signing algorithm.
 * Tokens include user ID, username, role, and expiration time.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret:MyVerySecureSecretKeyForJWTSigningWithMinimum32CharactersLength123456789}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:28800000}") // 8 hours in milliseconds (8 * 60 * 60 * 1000)
    private long jwtExpirationMs;

    /**
     * Generate a JWT token for a user
     * @param userId User's ID
     * @param username User's username
     * @param role User's role (UPLOADER or ADMIN)
     * @return JWT token string
     */
    public String generateToken(Long userId, String username, Role role) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("role", role.name());  // Store role as string (UPLOADER or ADMIN)

            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + jwtExpirationMs);

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(username)  // subject is the username
                    .setIssuedAt(now)
                    .setExpiration(expirationDate)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            log.info("JWT token generated for user: {}", username);
            return token;

        } catch (Exception e) {
            log.error("Error generating JWT token", e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Validate a JWT token
     * @param token JWT token string
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;

        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error validating JWT token", e);
        }

        return false;
    }

    /**
     * Extract all claims from a token
     * @param token JWT token string
     * @return Claims object containing all claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (Exception e) {
            log.error("Error extracting claims from JWT token", e);
            return null;
        }
    }

    /**
     * Extract username from token
     * @param token JWT token string
     * @return Username (subject of the token)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extract user ID from token
     * @param token JWT token string
     * @return User ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.containsKey("userId")) {
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        return null;
    }

    /**
     * Extract role from token
     * @param token JWT token string
     * @return User's role (UPLOADER or ADMIN)
     */
    public Role getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.containsKey("role")) {
            String roleStr = (String) claims.get("role");
            try {
                return Role.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role in token: {}", roleStr);
                return null;
            }
        }
        return null;
    }

    /**
     * Check if token is expired
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                return claims.getExpiration().before(new Date());
            }
        } catch (Exception e) {
            log.debug("Error checking token expiration", e);
        }
        return true;
    }

    /**
     * Get token expiration time
     * @param token JWT token string
     * @return Expiration date, or null if invalid
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }
}
