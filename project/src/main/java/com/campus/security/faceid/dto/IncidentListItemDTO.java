package com.campus.security.faceid.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for GET /incidents - incident list view
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentListItemDTO {
    private Long id;
    private LocalDateTime timestamp;
    private String mediaUrl;
    private String location;
    private String status;  // PENDING, CONFIRMED, REJECTED
    private Long reviewedByUserId;
    private LocalDateTime reviewedAt;
    private Long confirmedStudentId;
    private String confirmedStudentName;
    // Array of proposed matches
    private java.util.List<MatchProposalDTO> proposedMatches;
}
