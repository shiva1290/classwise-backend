package com.classwise.api.controller;

import com.classwise.api.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<?> createEnrollment(@RequestBody Map<String, Object> body) {
        try {
            Integer classId = body.get("classId") != null ? ((Number) body.get("classId")).intValue() : null;
            String studentId = (String) body.get("studentId");

            if (classId == null || studentId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId and studentId are required"));
            }

            Map<String, Object> result = enrollmentService.createEnrollment(classId, studentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", result));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create enrollment"));
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinClass(@RequestBody Map<String, Object> body) {
        try {
            String inviteCode = (String) body.get("inviteCode");
            String studentId = (String) body.get("studentId");

            if (inviteCode == null || studentId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "inviteCode and studentId are required"));
            }

            Map<String, Object> result = enrollmentService.joinClass(inviteCode, studentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", result));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to join class"));
        }
    }
}
