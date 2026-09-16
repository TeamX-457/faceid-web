package com.campus.security.faceid.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private String studentClass;

    // Stored as a comma-separated float string in Postgres (e.g. "0.123,0.456,...") - this is
    // always the front-facing enrollment photo's embedding, required for every student.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String embeddingVector;

    // Left/right profile angles are optional extras: a fight/crowd scene rarely gives a clean
    // frontal shot, so matching against side-profile embeddings too (see IncidentService's
    // gallery building) meaningfully improves recognition odds. Nullable since re-enrolling
    // every existing student with side photos isn't required for the front-only flow to work.
    @Column(columnDefinition = "TEXT", name = "left_embedding_vector")
    private String leftEmbeddingVector;

    @Column(columnDefinition = "TEXT", name = "right_embedding_vector")
    private String rightEmbeddingVector;

    @Column(name = "front_photo_path")
    private String frontPhotoPath;

    @Column(name = "left_photo_path")
    private String leftPhotoPath;

    @Column(name = "right_photo_path")
    private String rightPhotoPath;

    // Descriptive identity details beyond the biometric match itself - for a human reviewer
    // (teacher/admin) trying to visually pick a specific student out of a crowd of fighting
    // students, or to follow up with their family afterwards. None of this feeds the face
    // matching pipeline; it's purely for display once a match (or a manual lookup) is made.
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "skin_tone")
    private String skinTone;

    @Column(columnDefinition = "TEXT", name = "home_address")
    private String homeAddress;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Column(nullable = false, name = "enrollment_date")
    private LocalDateTime enrollmentDate;
}