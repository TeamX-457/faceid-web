package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatchResultDTO {
    private FaceBoxDTO faceBox;
    private Long matchedStudentId;
    private String matchedStudentName;
    private String matchedStudentClass;
    private double confidenceScore; // Cosine similarity score [0.0 - 1.0]
}