package com.campus.security.faceid.dto;

import lombok.*;

/**
 * Request DTO for POST /incidents/{id}/confirm
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmIncidentRequestDTO {
    private Long studentId;  // Which student the incident is confirmed to match
    private String notes;    // Optional notes/reason for confirmation
}
