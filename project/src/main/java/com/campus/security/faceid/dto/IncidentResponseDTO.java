package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncidentResponseDTO {
    private Long incidentId;
    private LocalDateTime timestamp;
    private String mediaPath;
    private List<MatchResultDTO> matches;
}