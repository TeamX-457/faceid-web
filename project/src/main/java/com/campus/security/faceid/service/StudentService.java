package com.campus.security.faceid.service;

import com.campus.security.faceid.dto.PythonGenerateEmbeddingResponse;
import com.campus.security.faceid.model.Student;
import com.campus.security.faceid.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
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

    /**
     * Enroll a new student by capturing their face embedding
     * 
     * @param fullName Student's full name
     * @param studentClass Student's class/grade
     * @param enrollmentPhoto Clear photo of the student's face
     * @return Newly created Student entity
     * @throws Exception if photo processing fails or service unavailable
     */
    public Student enrollStudent(
            String fullName,
            String studentClass,
            MultipartFile enrollmentPhoto) throws Exception {
        
        try {
            logger.info("Enrolling student: {} (class: {})", fullName, studentClass);
            
            // 1. Save enrollment photo temporarily
            String filename = "enrollment_" + UUID.randomUUID() + "_" + enrollmentPhoto.getOriginalFilename();
            File tempFile = new File(filename);
            enrollmentPhoto.transferTo(tempFile);
            
            logger.debug("Enrollment photo saved to: {}", tempFile.getAbsolutePath());
            
            // 2. Call Python service to generate embedding
            logger.info("Calling Python AI service to generate embedding");
            PythonGenerateEmbeddingResponse embeddingResponse = 
                    externalAIService.generateEmbedding(tempFile.getAbsolutePath());
            
            if (!embeddingResponse.isSuccess()) {
                throw new RuntimeException("Failed to generate embedding: " + embeddingResponse.getMessage());
            }
            
            // 3. Convert embedding list to comma-separated string
            String embeddingStr = embeddingResponse.getEmbedding().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            
            logger.debug("Generated embedding with {} dimensions", embeddingResponse.getEmbedding().size());
            
            // 4. Create and save student entity
            Student student = Student.builder()
                    .fullName(fullName)
                    .studentClass(studentClass)
                    .embeddingVector(embeddingStr)
                    .enrollmentDate(LocalDateTime.now())
                    .build();
            
            Student savedStudent = studentRepository.save(student);
            logger.info("Student enrolled successfully with ID: {}", savedStudent.getId());
            
            // 5. Clean up temporary file
            try {
                tempFile.delete();
            } catch (Exception e) {
                logger.warn("Failed to delete temporary enrollment photo: {}", e.getMessage());
            }
            
            return savedStudent;
            
        } catch (Exception e) {
            logger.error("Error enrolling student: {}", e.getMessage(), e);
            throw e;
        }
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
    public Student updateStudent(Long id, String fullName, String studentClass) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));

        student.setFullName(fullName);
        student.setStudentClass(studentClass);

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
