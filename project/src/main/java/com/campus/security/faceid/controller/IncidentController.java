package com.campus.security.faceid.controller;

import com.campus.security.faceid.dto.*;
import com.campus.security.faceid.model.Incident;
import com.campus.security.faceid.model.User;
import com.campus.security.faceid.repository.UserRepository;
import com.campus.security.faceid.security.JwtUserDetails;
import com.campus.security.faceid.service.AuditLogService;
import com.campus.security.faceid.service.IncidentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Incident Management Controller with role-based access control.
 * 
 * UPLOADER can:
 * - POST /incidents/upload - Upload incident footage
 * 
 * ADMIN can:
 * - GET /incidents - List all incidents
 * - GET /incidents/{id} - View full incident details
 * - POST /incidents/{id}/confirm - Confirm an incident match
 * - POST /incidents/{id}/reject - Reject an incident
 */
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {

    private final IncidentService incidentService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Upload incident media - UPLOADER ONLY
     * Triggers face detection and matching against the student gallery
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('UPLOADER')")
    public ResponseEntity<?> uploadIncidentMedia(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorMessage("File is empty"));
            }

            // Process incident image (face detection + matching)
            IncidentResponseDTO response = incidentService.processIncidentImage(file);

            log.info("Incident uploaded and processed successfully. Incident ID: {}", response.getIncidentId());

            // Log the action
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                auditLogService.logAction(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(),
                        "INCIDENT_UPLOADED", "INCIDENT", response.getIncidentId(), null, null, null);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing incident upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to process incident: " + e.getMessage()));
        }
    }

    /**
     * Get all incidents - ADMIN ONLY
     * Returns list with summary info (id, timestamp, status, proposed matches)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<IncidentListItemDTO>> getAllIncidents() {
        try {
            List<Incident> incidents = incidentService.getAllIncidents();
            List<IncidentListItemDTO> response = new ArrayList<>();

            for (Incident incident : incidents) {
                IncidentListItemDTO item = IncidentListItemDTO.builder()
                        .id(incident.getId())
                        .timestamp(incident.getTimestamp())
                        .mediaUrl("/uploads/" + fileName(incident.getMediaPath()))
                        .status(incident.getStatus())
                        .reviewedByUserId(incident.getReviewedByUserId())
                        .reviewedAt(incident.getReviewedAt())
                        .confirmedStudentId(incident.getConfirmedStudentId())
                        .proposedMatches(parseMatchResults(incident.getMatchResultsJson()))
                        .build();
                response.add(item);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving incidents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get incident details - ADMIN ONLY
     * Full details including all face detection boxes and match results
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getIncidentDetail(@PathVariable Long id) {
        try {
            Optional<Incident> incidentOpt = incidentService.getIncidentById(id);

            if (incidentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Incident incident = incidentOpt.get();

            // Get reviewed by username if applicable
            String reviewedByUsername = null;
            if (incident.getReviewedByUserId() != null) {
                Optional<User> reviewedByUser = userRepository.findById(incident.getReviewedByUserId());
                if (reviewedByUser.isPresent()) {
                    reviewedByUsername = reviewedByUser.get().getUsername();
                }
            }

            IncidentDetailResponseDTO response = IncidentDetailResponseDTO.builder()
                    .id(incident.getId())
                    .timestamp(incident.getTimestamp())
                    .mediaPath(incident.getMediaPath())
                    .detectedFacesJson(incident.getDetectedFacesJson())
                    .matchResultsJson(incident.getMatchResultsJson())
                    .status(incident.getStatus())
                    .reviewedByUserId(incident.getReviewedByUserId())
                    .reviewedByUsername(reviewedByUsername)
                    .reviewedAt(incident.getReviewedAt())
                    .confirmedStudentId(incident.getConfirmedStudentId())
                    .reviewNotes(incident.getReviewNotes())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving incident detail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Confirm an incident match - ADMIN ONLY
     * Marks incident as CONFIRMED and records which admin confirmed it
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirmIncident(@PathVariable Long id,
                                             @RequestBody ConfirmIncidentRequestDTO request) {
        try {
            if (request.getStudentId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("studentId is required"));
            }

            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("User not authenticated"));
            }

            // Update incident status
            Incident updatedIncident = incidentService.confirmIncident(
                    id,
                    request.getStudentId(),
                    currentUser.getId(),
                    request.getNotes()
            );

            // Log the action
            auditLogService.logAction(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(),
                    "INCIDENT_CONFIRMED", "INCIDENT", id,
                    Map.of("studentId", request.getStudentId(), "notes", request.getNotes()), null, null);

            log.info("Incident {} confirmed by admin {}", id, currentUser.getUsername());

            return ResponseEntity.ok(new SuccessMessage("Incident confirmed successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error confirming incident", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to confirm incident: " + e.getMessage()));
        }
    }

    /**
     * Reject an incident - ADMIN ONLY
     * Marks incident as REJECTED and records which admin rejected it
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectIncident(@PathVariable Long id,
                                           @RequestBody RejectIncidentRequestDTO request) {
        try {
            if (request.getReason() == null || request.getReason().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("reason is required"));
            }

            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorMessage("User not authenticated"));
            }

            // Update incident status
            Incident updatedIncident = incidentService.rejectIncident(
                    id,
                    currentUser.getId(),
                    request.getReason() + (request.getNotes() != null ? " - " + request.getNotes() : "")
            );

            // Log the action
            auditLogService.logAction(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(),
                    "INCIDENT_REJECTED", "INCIDENT", id,
                    Map.of("reason", request.getReason(), "notes", request.getNotes()), null, null);

            log.info("Incident {} rejected by admin {}", id, currentUser.getUsername());

            return ResponseEntity.ok(new SuccessMessage("Incident rejected successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error rejecting incident", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to reject incident: " + e.getMessage()));
        }
    }

    /**
     * Helper: Get current authenticated user
     */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtUserDetails) {
                JwtUserDetails details = (JwtUserDetails) auth.getDetails();
                Optional<User> user = userRepository.findById(details.getUserId());
                return user.orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not retrieve current user", e);
        }
        return null;
    }

    /**
     * Helper: Parse match results JSON into MatchProposalDTOs
     */
    private List<MatchProposalDTO> parseMatchResults(String matchResultsJson) {
        if (matchResultsJson == null || matchResultsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<MatchResultDTO> stored = objectMapper.readValue(
                    matchResultsJson, new com.fasterxml.jackson.core.type.TypeReference<List<MatchResultDTO>>() {});
            List<MatchProposalDTO> matches = new ArrayList<>();
            for (MatchResultDTO m : stored) {
                if (m.getMatchedStudentId() == null) continue;
                matches.add(MatchProposalDTO.builder()
                        .studentId(m.getMatchedStudentId())
                        .studentName(m.getMatchedStudentName())
                        .studentClass(m.getMatchedStudentClass())
                        .confidenceScore(m.getConfidenceScore())
                        .build());
            }
            return matches;
        } catch (Exception e) {
            log.debug("Error parsing match results JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * Helper: Extract just the filename from a stored media path
     */
    private String fileName(String mediaPath) {
        if (mediaPath == null) return "";
        String normalized = mediaPath.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    /**
     * Error response DTO
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ErrorMessage {
        private String message;
    }

    /**
     * Success response DTO
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class SuccessMessage {
        private String message;
    }
}