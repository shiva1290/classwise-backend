package com.classwise.api.controller;

import com.classwise.api.service.DepartmentService;
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
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<?> listDepartments(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(departmentService.listDepartments(search, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch departments"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Map<String, Object> body) {
        try {
            String code = (String) body.get("code");
            String name = (String) body.get("name");
            String description = (String) body.get("description");
            Map<String, Object> result = departmentService.createDepartment(code, name, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("data", result));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create department"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartment(@PathVariable Integer id) {
        try {
            Map<String, Object> result = departmentService.getDepartmentDetails(id);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Department not found"));
            }
            return ResponseEntity.ok(Map.of("data", result));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch department details"));
        }
    }

    @GetMapping("/{id}/subjects")
    public ResponseEntity<?> getDepartmentSubjects(
            @PathVariable Integer id,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(departmentService.getDepartmentSubjects(id, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch department subjects"));
        }
    }

    @GetMapping("/{id}/classes")
    public ResponseEntity<?> getDepartmentClasses(
            @PathVariable Integer id,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(departmentService.getDepartmentClasses(id, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch department classes"));
        }
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getDepartmentUsers(
            @PathVariable Integer id,
            @RequestParam(value = "role") String role,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            if (!"teacher".equals(role) && !"student".equals(role)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
            }
            return ResponseEntity.ok(departmentService.getDepartmentUsers(id, role, page, limit));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch department users"));
        }
    }
}
