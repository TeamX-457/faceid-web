package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Everything captured at student enrollment time. Only fullName, studentClass, and frontPhoto
 * are required - the rest are optional descriptive/matching extras (see Student entity).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnrollStudentRequest {
    private String fullName;
    private String studentClass;
    private LocalDate dateOfBirth;
    private Integer heightCm;
    private String skinTone;
    private String homeAddress;
    private String guardianPhone;

    private MultipartFile frontPhoto;
    private MultipartFile leftPhoto;
    private MultipartFile rightPhoto;
}
