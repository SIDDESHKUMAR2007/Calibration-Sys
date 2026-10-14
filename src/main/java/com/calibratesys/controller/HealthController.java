package com.calibratesys.controller;

import com.calibratesys.dto.HealthResponseDto;
import com.calibratesys.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing foundation health and system status endpoints.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    @Autowired
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Endpoint to check backend service health and database connectivity.
     *
     * @return ResponseEntity with HealthResponseDto
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> getHealth() {
        HealthResponseDto health = healthService.checkHealth();
        return ResponseEntity.ok(health);
    }
}
