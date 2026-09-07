package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response from Python microservice /generate-embedding endpoint
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PythonGenerateEmbeddingResponse {
    private boolean success;
    private List<Float> embedding;
    private String message;
}
