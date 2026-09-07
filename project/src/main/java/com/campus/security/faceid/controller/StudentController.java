package com.campus.security.faceid.controller;

import com.campus.security.faceid.model.Student;
import com.campus.security.faceid.model.User;
import com.campus.security.faceid.repository.UserRepository;
import com.campus.security.faceid.security.JwtUserDetails;
import com.campus.security.faceid.service.AuditLogService;
import com.campus.security.faceid.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Student Management Controller with role-based access control.
 * ADMIN ONLY - Full CRUD access to student roster
 * UPLOADER has zero access
 * 
 * Endpoints:
 * - GET /students - List all enrolled students
 * - GET /students/{id} - View student details
 * - POST /students - Enroll new student with face embedding
 * - PUT /students/{id} - Update student info
 * - DELETE /students/{id} - Remove student from roster
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    /**
     * Get all enrolled students - ADMIN ONLY
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Student>> getAllStudents() {
        try {
            List<Student> students = studentService.getAllStudents();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            log.error("Error retrieving students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get student by ID - ADMIN ONLY
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStudent(@PathVariable Long id) {
        try {
            Optional<Student> student = studentService.getStudentById(id);
            if (student.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(student.get());
        } catch (Exception e) {
            log.error("Error retrieving student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Enroll a new student with face embedding - ADMIN ONLY
     * Request: multipart/form-data
     * - full_name: Student's full name
     * - student_class: Student's class/grade
     * - enrollment_photo: Image file of student's face
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enrollStudent(
            @RequestParam("full_name") String fullName,
            @RequestParam("student_class") String studentClass,
            @RequestParam("enrollment_photo") MultipartFile enrollmentPhoto) {
        try {
            // Validate input
            if (fullName == null || fullName.isBlank() ||
                studentClass == null || studentClass.isBlank() ||
                enrollmentPhoto == null || enrollmentPhoto.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("full_name, student_class, and enrollment_photo are required"));
            }

            // Enroll student (generates embedding via Python service)
            Student newStudent = studentService.enrollStudent(fullName, studentClass, enrollmentPhoto);

            // Get current user for audit log
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                auditLogService.logAction(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(),
                        "STUDENT_ENROLLED", "STUDENT", newStudent.getId(),
                        Map.of("fullName", fullName, "studentClass", studentClass), null, null);
            }

            log.info("Student enrolled: {} (ID: {})", fullName, newStudent.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newStudent);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        } catch (Exception e) {
            log.error("Error enrolling student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to enroll student: " + e.getMessage()));
        }
    }

    /**
     * Update student information - ADMIN ONLY
     * Request: {full_name, student_class}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStudent(@PathVariable Long id,
                                          @RequestBody UpdateStudentRequest request) {
        try {
            // Validate input
            if (request.getFullName() == null || request.getFullName().isBlank() ||
                request.getStudentClass() == null || request.getStudentClass().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorMessage("full_name and student_class are required"));
            }

            // Update student
            Student updatedStudent = studentService.updateStudent(
                    id,
                    request.getFullName(),
                    request.getStudentClass()
            );

            // Get current user for audit log
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                auditLogService.logAction(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(),
                        "STUDENT_UPDATED", "STUDENT", id,
                        Map.of("fullName", request.getFullName(), "studentClass", request.getStudentClass()), null, null);
            }

            log.info("Student updated: ID {}", id);
            return ResponseEntity.ok(updatedStudent);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to update student: " + e.getMessage()));
        }
    }

    /**
     * Delete a student from roster - ADMIN ONLY
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            studentService.deleteStudent(id);

            // Get current user for audit log
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                auditLogService.logAction(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(),
                        "STUDENT_DELETED", "STUDENT", id, null, null, null);
            }

            log.info("Student deleted: ID {}", id);
            return ResponseEntity.ok(new SuccessMessage("Student deleted successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorMessage("Failed to delete student: " + e.getMessage()));
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
     * Update student request DTO
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdateStudentRequest {
        private String fullName;
        private String studentClass;
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
