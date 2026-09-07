package com.campus.security.faceid.dto;

import lombok.*;

/**
 * Request DTO for POST /incidents/{id}/reject
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectIncidentRequestDTO {
    private String reason;  // Reason for rejection
    private String notes;   // Optional additional notes
}
