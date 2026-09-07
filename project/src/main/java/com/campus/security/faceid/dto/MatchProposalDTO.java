package com.campus.security.faceid.dto;

import lombok.*;

/**
 * DTO for a single match proposal in an incident
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchProposalDTO {
    private Long studentId;
    private String studentName;
    private String studentClass;
    private Double confidenceScore;
}
