package com.campus.security.faceid.service;

import com.campus.security.faceid.model.AuditLog;
import com.campus.security.faceid.model.Role;
import com.campus.security.faceid.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for audit logging.
 * Every sensitive action (incident confirmation/rejection, student enrollment/deletion, user management)
 * is logged with user details, action type, affected entity, and request metadata (IP, User-Agent).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log an action to the audit log
     * @param userId ID of the user performing the action
     * @param username Username of the user
     * @param userRole Role of the user
     * @param action Action type (e.g., "INCIDENT_CONFIRMED", "STUDENT_ENROLLED")
     * @param entityType Type of entity affected (e.g., "INCIDENT", "STUDENT", "USER")
     * @param entityId ID of the entity being acted upon
     * @param details Additional context (as Map, will be serialized to JSON)
     * @param ipAddress IP address of the request
     * @param userAgent User-Agent of the request
     */
    public AuditLog logAction(Long userId, String username, Role userRole, String action,
                               String entityType, Long entityId, Map<String, Object> details,
                               String ipAddress, String userAgent) {
        try {
            // Auto-capture IP and User-Agent from current HTTP request if not provided
            if (ipAddress == null || userAgent == null) {
                HttpServletRequest request = getHttpServletRequest();
                if (request != null) {
                    if (ipAddress == null) {
                        ipAddress = getClientIp(request);
                    }
                    if (userAgent == null) {
                        userAgent = request.getHeader("User-Agent");
                    }
                }
            }

            // Serialize details map to JSON
            String detailsJson = null;
            if (details != null) {
                detailsJson = objectMapper.writeValueAsString(details);
            }

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .userRole(userRole)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(detailsJson)
                    .timestamp(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent != null ? userAgent.substring(0, Math.min(500, userAgent.length())) : null)
                    .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} - {} [{}]", action, entityType, entityId);
            return saved;

        } catch (Exception e) {
            log.error("Error creating audit log: {}", action, e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }

    /**
     * Get audit logs for a specific user
     */
    public List<AuditLog> getAuditLogsByUser(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    /**
     * Get audit logs for a specific entity
     */
    public List<AuditLog> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get audit logs for a specific action type
     */
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    /**
     * Get recent audit logs
     */
    public List<AuditLog> getRecentAuditLogs(int limit) {
        return auditLogRepository.findRecentAuditLogs(limit);
    }

    /**
     * Extract client IP address from HttpServletRequest
     * Handles X-Forwarded-For header and multiple proxies
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs; take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Get the current HttpServletRequest from Spring context
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("Could not retrieve HttpServletRequest from context");
        }
        return null;
    }
}
