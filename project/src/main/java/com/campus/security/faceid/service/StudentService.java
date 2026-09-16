package com.campus.security.faceid.service;

import com.campus.security.faceid.dto.EnrollStudentRequest;
import com.campus.security.faceid.dto.PythonGenerateEmbeddingResponse;
import com.campus.security.faceid.model.Student;
import com.campus.security.faceid.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing student enrollment
 * Generates and stores face embeddings for students
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    private final ExternalAIService externalAIService;
    private final StudentRepository studentRepository;

    // Same permanent directory (and /uploads/** static serving) already used for incident
    // media - see IncidentService and WebConfig.
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private record AnglePhoto(String filename, String embedding) {}

    /**
     * Enroll a new student, generating one face embedding per provided angle. Front is
     * required; left/right profile shots are optional but meaningfully improve match odds
     * against off-angle security footage (see IncidentService's gallery building).
     *
     * @return Newly created Student entity
     * @throws Exception if photo processing fails or the AI service is unavailable
     */
    public Student enrollStudent(EnrollStudentRequest request) throws Exception {
        try {
            logger.info("Enrolling student: {} (class: {})", request.getFullName(), request.getStudentClass());

            AnglePhoto front = processAngle(request.getFrontPhoto(), "front");
            if (front == null) {
                throw new IllegalArgumentException("front_photo is required");
            }
            AnglePhoto left = processAngle(request.getLeftPhoto(), "left");
            AnglePhoto right = processAngle(request.getRightPhoto(), "right");

            Student student = Student.builder()
                    .fullName(request.getFullName())
                    .studentClass(request.getStudentClass())
                    .dateOfBirth(request.getDateOfBirth())
                    .heightCm(request.getHeightCm())
                    .skinTone(request.getSkinTone())
                    .homeAddress(request.getHomeAddress())
                    .guardianPhone(request.getGuardianPhone())
                    .embeddingVector(front.embedding())
                    .frontPhotoPath(front.filename())
                    .leftEmbeddingVector(left != null ? left.embedding() : null)
                    .leftPhotoPath(left != null ? left.filename() : null)
                    .rightEmbeddingVector(right != null ? right.embedding() : null)
                    .rightPhotoPath(right != null ? right.filename() : null)
                    .enrollmentDate(LocalDateTime.now())
                    .build();

            Student savedStudent = studentRepository.save(student);
            logger.info("Student enrolled successfully with ID: {}", savedStudent.getId());
            return savedStudent;

        } catch (Exception e) {
            logger.error("Error enrolling student: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Saves one angle's photo permanently (so it can be displayed later, unlike the old
     * temp-file-then-discard flow) and generates its embedding. Returns null if no photo was
     * provided for this angle - left/right are optional.
     */
    private AnglePhoto processAngle(MultipartFile photo, String angleLabel) throws Exception {
        if (photo == null || photo.isEmpty()) {
            return null;
        }

        File dir = new File(uploadDir).getAbsoluteFile();
        if (!dir.exists()) dir.mkdirs();

        String filename = "student_" + UUID.randomUUID() + "_" + angleLabel + "_" + sanitizeFilename(photo.getOriginalFilename());
        File destFile = new File(dir, filename);
        photo.transferTo(destFile);

        logger.debug("{} photo saved to: {}", angleLabel, destFile.getAbsolutePath());

        PythonGenerateEmbeddingResponse embeddingResponse =
                externalAIService.generateEmbedding(destFile.getAbsolutePath());

        if (!embeddingResponse.isSuccess()) {
            throw new RuntimeException("Failed to generate " + angleLabel + " embedding: " + embeddingResponse.getMessage());
        }

        String embeddingStr = embeddingResponse.getEmbedding().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        logger.debug("Generated {} embedding with {} dimensions", angleLabel, embeddingResponse.getEmbedding().size());
        return new AnglePhoto(filename, embeddingStr);
    }

    private String sanitizeFilename(String name) {
        return name == null ? "photo.jpg" : name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Get all enrolled students
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Get a student by ID
     */
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    /**
     * Update student information
     * @param id Student ID
     * @param fullName New full name
     * @param studentClass New class/grade
     * @return Updated student
     */
    public Student updateStudent(
            Long id,
            String fullName,
            String studentClass,
            java.time.LocalDate dateOfBirth,
            Integer heightCm,
            String skinTone,
            String homeAddress,
            String guardianPhone) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));

        student.setFullName(fullName);
        student.setStudentClass(studentClass);
        student.setDateOfBirth(dateOfBirth);
        student.setHeightCm(heightCm);
        student.setSkinTone(skinTone);
        student.setHomeAddress(homeAddress);
        student.setGuardianPhone(guardianPhone);

        Student updated = studentRepository.save(student);
        logger.info("Student updated: ID {}", id);

        return updated;
    }

    /**
     * Delete a student from the roster
     * @param id Student ID
     */
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));

        studentRepository.delete(student);
        logger.info("Student deleted: ID {}", id);
    }
}
