package com.campus.security.faceid.service;

import com.campus.security.faceid.dto.*;
import com.campus.security.faceid.model.Incident;
import com.campus.security.faceid.model.Student;
import com.campus.security.faceid.repository.IncidentRepository;
import com.campus.security.faceid.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates incident processing by delegating to the Python AI microservice
 * Handles face detection and matching via external Python service
 */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentService.class);

    private final ExternalAIService externalAIService;
    private final StudentRepository studentRepository;
    private final IncidentRepository incidentRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Process incident image by sending to Python microservice for face detection and matching
     * 
     * @param file Uploaded incident image
     * @return Incident response with match results
     * @throws Exception if image processing fails or AI service is unavailable
     */
    public IncidentResponseDTO processIncidentImage(MultipartFile file) throws Exception {
        try {
            // 1. Save uploaded file to disk
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File destFile = new File(dir, filename);
            file.transferTo(destFile);
            
            logger.info("Image saved to: {}", destFile.getAbsolutePath());

            // 2. Get all enrolled students' embeddings from database
            List<Student> enrolledStudents = studentRepository.findAll();
            
            if (enrolledStudents.isEmpty()) {
                logger.warn("No enrolled students found in database");
            } else {
                logger.info("Loaded {} enrolled students for gallery matching", enrolledStudents.size());
            }
            
            // 3. Convert student embeddings to gallery format
            List<GalleryEmbeddingDTO> galleryEmbeddings = enrolledStudents.stream()
                    .map(student -> {
                        List<Float> embedding = parseEmbedding(student.getEmbeddingVector());
                        return GalleryEmbeddingDTO.builder()
                                .studentId(student.getId())
                                .name(student.getFullName())
                                .studentClass(student.getStudentClass())
                                .embedding(embedding)
                                .build();
                    })
                    .collect(Collectors.toList());

            // 4. Call Python AI microservice for detection and matching
            logger.info("Calling Python AI service for face detection and matching");
            PythonDetectAndMatchResponse pythonResponse = externalAIService.detectAndMatchFaces(
                    destFile.getAbsolutePath(),
                    galleryEmbeddings
            );
            
            logger.info("Python service detected {} faces with {} matches",
                    pythonResponse.getDetectedFacesCount(),
                    pythonResponse.getMatches().size());

            // 5. Convert Python response to MatchResultDTO for storage
            List<MatchResultDTO> matchResults = pythonResponse.getMatches().stream()
                    .map(pythonMatch -> MatchResultDTO.builder()
                            .faceBox(pythonMatch.getFaceBox())
                            .matchedStudentId(pythonMatch.getMatchedStudentId())
                            .matchedStudentName(pythonMatch.getMatchedStudentName())
                            .matchedStudentClass(pythonMatch.getMatchedStudentClass())
                            .confidenceScore(pythonMatch.getConfidenceScore())
                            .build())
                    .collect(Collectors.toList());

            // 6. Save incident audit log to database
            Incident incident = Incident.builder()
                    .timestamp(LocalDateTime.now())
                    .mediaPath(destFile.getAbsolutePath())
                    .detectedFacesJson(objectMapper.writeValueAsString(pythonResponse.getMatches().stream()
                            .map(PythonMatchResult::getFaceBox)
                            .collect(Collectors.toList())))
                    .matchResultsJson(objectMapper.writeValueAsString(matchResults))
                    .build();

            Incident savedIncident = incidentRepository.save(incident);
            logger.info("Incident saved with ID: {}", savedIncident.getId());

            // 7. Return response
            return IncidentResponseDTO.builder()
                    .incidentId(savedIncident.getId())
                    .timestamp(savedIncident.getTimestamp())
                    .mediaPath(savedIncident.getMediaPath())
                    .matches(matchResults)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error processing incident image: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Parse comma-separated embedding string to List<Float>
     */
    private List<Float> parseEmbedding(String embeddingStr) {
        if (embeddingStr == null || embeddingStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(embeddingStr.split(","))
                .map(String::trim)
                .map(Float::parseFloat)
                .collect(Collectors.toList());
    }

    /**
     * Get all incidents
     */
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    /**
     * Get incident by ID
     */
    public Optional<Incident> getIncidentById(Long id) {
        return incidentRepository.findById(id);
    }

    /**
     * Confirm an incident match
     * @param incidentId ID of the incident
     * @param studentId ID of the confirmed student
     * @param adminUserId ID of the admin confirming
     * @param notes Optional notes/reason
     * @return Updated incident
     */
    public Incident confirmIncident(Long incidentId, Long studentId, Long adminUserId, String notes) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        incident.setStatus("CONFIRMED");
        incident.setConfirmedStudentId(studentId);
        incident.setReviewedByUserId(adminUserId);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(notes);

        Incident updated = incidentRepository.save(incident);
        logger.info("Incident {} confirmed by admin (user ID: {}) for student {}", incidentId, adminUserId, studentId);

        return updated;
    }

    /**
     * Reject an incident
     * @param incidentId ID of the incident
     * @param adminUserId ID of the admin rejecting
     * @param reason Reason for rejection
     * @return Updated incident
     */
    public Incident rejectIncident(Long incidentId, Long adminUserId, String reason) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        incident.setStatus("REJECTED");
        incident.setReviewedByUserId(adminUserId);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewNotes(reason);

        Incident updated = incidentRepository.save(incident);
        logger.info("Incident {} rejected by admin (user ID: {}). Reason: {}", incidentId, adminUserId, reason);

        return updated;
    }
}