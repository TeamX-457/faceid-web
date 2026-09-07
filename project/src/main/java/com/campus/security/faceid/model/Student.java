package com.campus.security.faceid.model;

import jakarta.persistence.*;
import lombok.*;
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

    // Stored as a comma-separated float string in Postgres (e.g. "0.123,0.456,...")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String embeddingVector;

    @Column(nullable = false, name = "enrollment_date")
    private LocalDateTime enrollmentDate;
}