package com.classwise.api.controller;

import com.classwise.api.service.ClassService;
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
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public ResponseEntity<?> listClasses(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "teacher", required = false) String teacher,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(classService.listClasses(search, subject, teacher, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch classes"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClass(@PathVariable Integer id) {
        try {
            Map<String, Object> result = classService.getClassDetails(id);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Class not found"));
            }
            return ResponseEntity.ok(Map.of("data", result));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch class details"));
        }
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getClassUsers(
            @PathVariable Integer id,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(classService.getClassUsers(id, role, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch class users"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createClass(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String teacherId = (String) body.get("teacherId");
            Integer subjectId = body.get("subjectId") != null ? ((Number) body.get("subjectId")).intValue() : null;
            Integer capacity = body.get("capacity") != null ? ((Number) body.get("capacity")).intValue() : null;
            String description = (String) body.get("description");
            String status = (String) body.get("status");
            String bannerUrl = (String) body.get("bannerUrl");
            String bannerCldPubId = (String) body.get("bannerCldPubId");
            Object schedules = body.get("schedules");

            Map<String, Object> result = classService.createClass(
                    name, teacherId, subjectId, capacity, description,
                    status, bannerUrl, bannerCldPubId, schedules);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", result));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create class"));
        }
    }
}
