package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Match result from Python service
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PythonMatchResult {
    private FaceBoxDTO faceBox;
    private Long matchedStudentId;
    private String matchedStudentName;
    private String matchedStudentClass;
    private double confidenceScore;
}
