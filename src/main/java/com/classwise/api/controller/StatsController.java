package com.classwise.api.controller;

import com.classwise.api.service.StatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        try {
            return ResponseEntity.ok(Map.of("data", statsService.getOverview()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch overview stats"));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<?> latest(@RequestParam(value = "limit", defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(Map.of("data", statsService.getLatest(limit)));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch latest stats"));
        }
    }

    @GetMapping("/charts")
    public ResponseEntity<?> charts() {
        try {
            return ResponseEntity.ok(Map.of("data", statsService.getCharts()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch chart stats"));
        }
    }
}
