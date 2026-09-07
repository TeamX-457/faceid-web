package com.campus.security.faceid.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * AuditLog entity for tracking all sensitive actions in the system.
 * Every state change (confirm/reject/enroll/delete) is logged with user, action, target, and timestamp.
 * This provides a complete audit trail for security and compliance.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100, name = "username")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role userRole;

    @Column(nullable = false, length = 100, name = "action")
    private String action;  // e.g., "INCIDENT_CONFIRMED", "STUDENT_ENROLLED", "USER_DELETED"

    @Column(length = 50, name = "entity_type")
    private String entityType;  // e.g., "INCIDENT", "STUDENT", "USER"

    @Column(name = "entity_id")
    private Long entityId;  // ID of the entity being acted upon

    @Column(columnDefinition = "TEXT", name = "details")
    private String details;  // JSON payload with additional context

    @Column(nullable = false, name = "timestamp")
    private LocalDateTime timestamp;

    @Column(length = 50, name = "ip_address")
    private String ipAddress;

    @Column(length = 500, name = "user_agent")
    private String userAgent;

    /**
     * Automatically set timestamp on entity creation
     */
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
