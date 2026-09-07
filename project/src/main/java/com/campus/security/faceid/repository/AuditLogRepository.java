package com.campus.security.faceid.repository;

import com.campus.security.faceid.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific user
     */
    List<AuditLog> findByUserId(Long userId);

    /**
     * Find all audit logs for a specific entity (e.g., all actions on a specific incident)
     */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * Find all audit logs for a specific action type
     */
    List<AuditLog> findByAction(String action);

    /**
     * Find audit logs within a date range
     */
    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startTime AND :endTime")
    List<AuditLog> findByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Find recent audit logs (last N entries)
     */
    @Query(value = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<AuditLog> findRecentAuditLogs(@Param("limit") int limit);
}
