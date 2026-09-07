package com.campus.security.faceid.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for GET /incidents/{id} - detailed incident view
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentDetailResponseDTO {
    private Long id;
    private LocalDateTime timestamp;
    private String mediaPath;
    private String detectedFacesJson;
    private String matchResultsJson;
    private String status;  // PENDING, CONFIRMED, REJECTED
    private Long reviewedByUserId;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private Long confirmedStudentId;
    private String confirmedStudentName;
    private String reviewNotes;
}
