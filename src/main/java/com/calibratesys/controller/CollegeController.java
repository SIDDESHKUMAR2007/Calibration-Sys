package com.calibratesys.controller;

import com.calibratesys.dto.CollegeRequestDto;
import com.calibratesys.dto.CollegeResponseDto;
import com.calibratesys.service.CollegeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for College management operations.
 * Write operations (create, update, delete) are restricted to ADMIN role.
 * Read operations are accessible to all authenticated users.
 */
@RestController
@RequestMapping("/api/colleges")
public class CollegeController {

    private final CollegeService collegeService;

    @Autowired
    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    /**
     * List all registered colleges.
     */
    @GetMapping
    public ResponseEntity<List<CollegeResponseDto>> getAllColleges() {
        List<CollegeResponseDto> colleges = collegeService.getAllColleges();
        return ResponseEntity.ok(colleges);
    }

    /**
     * Retrieve a specific college by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CollegeResponseDto> getCollegeById(@PathVariable Long id) {
        CollegeResponseDto college = collegeService.getCollegeById(id);
        return ResponseEntity.ok(college);
    }

    /**
     * Create a new college (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CollegeResponseDto> createCollege(@Valid @RequestBody CollegeRequestDto request) {
        CollegeResponseDto created = collegeService.createCollege(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing college by ID (ADMIN only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CollegeResponseDto> updateCollege(@PathVariable Long id,
                                                            @Valid @RequestBody CollegeRequestDto request) {
        CollegeResponseDto updated = collegeService.updateCollege(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a college by ID (ADMIN only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCollege(@PathVariable Long id) {
        collegeService.deleteCollege(id);
        return ResponseEntity.noContent().build();
    }
}
