package com.classwise.api.controller;

import com.classwise.api.service.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public ResponseEntity<?> listSubjects(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(subjectService.listSubjects(search, department, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subjects"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createSubject(@RequestBody Map<String, Object> body) {
        try {
            Integer departmentId = body.get("departmentId") != null ? ((Number) body.get("departmentId")).intValue()
                    : null;
            String name = (String) body.get("name");
            String code = (String) body.get("code");
            String description = (String) body.get("description");
            Map<String, Object> result = subjectService.createSubject(departmentId, name, code, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", result));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create subject"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSubject(@PathVariable Integer id) {
        try {
            Map<String, Object> result = subjectService.getSubjectDetails(id);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
            }
            return ResponseEntity.ok(Map.of("data", result));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subject details"));
        }
    }

    @GetMapping("/{id}/classes")
    public ResponseEntity<?> getSubjectClasses(
            @PathVariable Integer id,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(subjectService.getSubjectClasses(id, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subject classes"));
        }
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getSubjectUsers(
            @PathVariable Integer id,
            @RequestParam(value = "role") String role,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            if (!"teacher".equals(role) && !"student".equals(role)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
            }
            return ResponseEntity.ok(subjectService.getSubjectUsers(id, role, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subject users"));
        }
    }
}
