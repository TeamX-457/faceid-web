package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Gallery embedding entry to send to Python microservice
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GalleryEmbeddingDTO {
    private Long studentId;
    private String name;
    private String studentClass;
    private List<Float> embedding;  // Parsed from stored string
}
