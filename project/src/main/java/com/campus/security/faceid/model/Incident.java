package com.campus.security.faceid.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "timestamp")
    private LocalDateTime timestamp;

    @Column(nullable = false, name = "media_path")
    private String mediaPath;

    // Which uploader submitted this incident (user_id reference) - lets an UPLOADER list their own history
    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    // Optional free-text note the uploader attached at submission time (e.g. "Fight near block C")
    @Column(columnDefinition = "TEXT", name = "notes")
    private String notes;

    // Optional site/location name the uploader attached at submission time (e.g. "Main Gate").
    // Null for incidents submitted before this field existed - shown as "Unspecified", not hidden.
    @Column(name = "location")
    private String location;

    // JSON formatted list of detected bounding boxes
    @Column(columnDefinition = "TEXT", name = "detected_faces")
    private String detectedFacesJson;

    // JSON formatted match results and student details
    @Column(columnDefinition = "TEXT", name = "match_results")
    private String matchResultsJson;

    // Incident status: PENDING (initial), CONFIRMED (admin reviewed and confirmed), REJECTED (admin reviewed and rejected)
    @Column(nullable = false, name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";

    // Which admin confirmed/rejected this incident (user_id reference)
    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    // When the incident was reviewed/confirmed/rejected
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // Which student the admin confirmed matched (if confirmed)
    @Column(name = "confirmed_student_id")
    private Long confirmedStudentId;

    // Notes/reason for rejection or confirmation
    @Column(columnDefinition = "TEXT", name = "review_notes")
    private String reviewNotes;
}