package com.campus.security.faceid.service;

import com.campus.security.faceid.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Service for communicating with the Python AI microservice
 * Handles face detection, embedding generation, and face matching
 */
@Service
@RequiredArgsConstructor
public class ExternalAIService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalAIService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.python-service-url:http://localhost:8001}")
    private String pythonServiceUrl;

    /**
     * Call the Python microservice to detect and match faces
     * 
     * @param imagePath Path to the incident image
     * @param galleryEmbeddings List of enrolled student embeddings
     * @return Match results from Python service
     * @throws Exception if service is unreachable or returns error
     */
    public PythonDetectAndMatchResponse detectAndMatchFaces(
            String imagePath,
            List<GalleryEmbeddingDTO> galleryEmbeddings) throws Exception {
        
        try {
            String detectionUrl = pythonServiceUrl + "/detect-and-match";
            
            // Prepare multipart form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(new File(imagePath)));
            
            // Convert gallery embeddings to JSON and add as form parameter
            String galleryJson = objectMapper.writeValueAsString(galleryEmbeddings);
            body.add("gallery_embeddings", galleryJson);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            
            // Make request
            logger.info("Calling Python service: {} with {} gallery embeddings", 
                    detectionUrl, galleryEmbeddings.size());
            
            PythonDetectAndMatchResponse response = restTemplate.postForObject(
                    detectionUrl,
                    request,
                    PythonDetectAndMatchResponse.class
            );
            
            logger.info("Python service returned {} matches", 
                    response.getMatches().size());
            
            return response;
            
        } catch (RestClientException e) {
            logger.error("Failed to communicate with Python AI service at {}: {}", 
                    pythonServiceUrl, e.getMessage());
            throw new RuntimeException(
                    "AI service is currently unavailable. Please try again later.", e);
        } catch (Exception e) {
            logger.error("Error calling Python AI service: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Call the Python microservice to generate a face embedding
     * Used during student enrollment
     * 
     * @param enrollmentImagePath Path to the student enrollment photo
     * @return Embedding vector from Python service
     * @throws Exception if service is unreachable or no face detected
     */
    public PythonGenerateEmbeddingResponse generateEmbedding(String enrollmentImagePath) throws Exception {
        try {
            String embeddingUrl = pythonServiceUrl + "/generate-embedding";
            
            // Prepare multipart form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(new File(enrollmentImagePath)));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            
            // Make request
            logger.info("Calling Python service to generate embedding: {}", embeddingUrl);
            
            PythonGenerateEmbeddingResponse response = restTemplate.postForObject(
                    embeddingUrl,
                    request,
                    PythonGenerateEmbeddingResponse.class
            );
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Python service failed: " + response.getMessage());
            }
            
            logger.info("Embedding generated successfully");
            return response;

        } catch (HttpClientErrorException e) {
            // A 4xx here is Python validating the photo (e.g. no face detected), not the
            // service being down - surface its actual reason rather than a generic message.
            logger.warn("Python service rejected the enrollment photo: {}", e.getResponseBodyAsString());
            throw new RuntimeException(extractDetail(e));
        } catch (RestClientException e) {
            logger.error("Failed to communicate with Python AI service at {}: {}",
                    pythonServiceUrl, e.getMessage());
            throw new RuntimeException(
                    "AI service is currently unavailable. Please try again later.", e);
        } catch (Exception e) {
            logger.error("Error generating embedding: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Pulls the {"detail": "..."} message out of a FastAPI error response, falling back to the
     * raw body if it isn't in that shape.
     */
    private String extractDetail(HttpClientErrorException e) {
        try {
            Map<String, Object> body = objectMapper.readValue(e.getResponseBodyAsString(), Map.class);
            Object detail = body.get("detail");
            if (detail != null) {
                return detail.toString();
            }
        } catch (Exception ignored) {
            // Not JSON, or not in the expected shape - fall through to the raw body.
        }
        return e.getResponseBodyAsString();
    }

    /**
     * Check if the Python AI microservice is healthy and reachable
     * 
     * @return true if service is reachable, false otherwise
     */
    public boolean isServiceHealthy() {
        try {
            String healthUrl = pythonServiceUrl + "/health";
            restTemplate.getForObject(healthUrl, Object.class);
            logger.debug("Python AI service is healthy");
            return true;
        } catch (Exception e) {
            logger.warn("Python AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
