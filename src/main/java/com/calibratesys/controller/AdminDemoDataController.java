package com.calibratesys.controller;

import com.calibratesys.service.DemoDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Administrative controller for managing demonstration dataset in development and evaluation environments.
 * Strictly restricted to users with ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/demo-data")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDemoDataController {

    private final DemoDataService demoDataService;

    @Autowired
    public AdminDemoDataController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDemoData() {
        Map<String, Long> counts = demoDataService.resetDemoData();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Demonstration dataset has been successfully reset to compact college-demo baseline.",
                "counts", counts
        ));
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Object>> getCounts() {
        Map<String, Long> counts = demoDataService.getEntityCounts();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "counts", counts
        ));
    }
}
