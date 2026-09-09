package com.campus.security.faceid.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response for POST /incidents/upload. Snake-cased to match the Android app's
 * IncidentUploadResponse (incident_id, status, uploaded_at are the fields it actually reads);
 * mediaPath and matches are extras the admin web dashboard's Scan Terminal uses, harmlessly
 * ignored by the mobile client.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IncidentResponseDTO {
    private Long incidentId;
    private String status;
    private LocalDateTime uploadedAt;
    private String mediaPath;
    private List<IdentifyMatchDTO> matches;
}
