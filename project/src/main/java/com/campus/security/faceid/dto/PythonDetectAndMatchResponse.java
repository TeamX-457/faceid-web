package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response from the Python AI microservice for detect-and-match
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PythonDetectAndMatchResponse {
    private boolean success;
    private int detectedFacesCount;
    private List<PythonMatchResult> matches;
    private String message;
}
