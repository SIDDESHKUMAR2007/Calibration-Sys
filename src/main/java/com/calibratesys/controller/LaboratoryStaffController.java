package com.calibratesys.controller;

import com.calibratesys.dto.LaboratoryStaffRequestDto;
import com.calibratesys.dto.LaboratoryStaffResponseDto;
import com.calibratesys.dto.LaboratoryStaffUpdateDto;
import com.calibratesys.service.LaboratoryStaffService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Laboratory Staff Assignment Management.
 * Read operations are accessible to all authenticated users.
 * Assignment creation, updates, and removals are restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/laboratories/{laboratoryId}/staff")
public class LaboratoryStaffController {

    private final LaboratoryStaffService laboratoryStaffService;

    @Autowired
    public LaboratoryStaffController(LaboratoryStaffService laboratoryStaffService) {
        this.laboratoryStaffService = laboratoryStaffService;
    }

    /**
     * List all staff members assigned to a specific laboratory.
     */
    @GetMapping
    public ResponseEntity<List<LaboratoryStaffResponseDto>> getStaffByLaboratory(@PathVariable Long laboratoryId) {
        List<LaboratoryStaffResponseDto> staffAssignments = laboratoryStaffService.getStaffByLaboratory(laboratoryId);
        return ResponseEntity.ok(staffAssignments);
    }

    /**
     * Assign a staff member to a laboratory (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LaboratoryStaffResponseDto> assignStaffToLaboratory(
            @PathVariable Long laboratoryId,
            @Valid @RequestBody LaboratoryStaffRequestDto request) {
        LaboratoryStaffResponseDto created = laboratoryStaffService.assignStaffToLaboratory(laboratoryId, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing staff assignment in a laboratory (ADMIN only).
     */
    @PutMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LaboratoryStaffResponseDto> updateStaffAssignment(
            @PathVariable Long laboratoryId,
            @PathVariable Long staffId,
            @Valid @RequestBody LaboratoryStaffUpdateDto request) {
        LaboratoryStaffResponseDto updated = laboratoryStaffService.updateStaffAssignment(laboratoryId, staffId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove a staff member from a laboratory (ADMIN only).
     */
    @DeleteMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeStaffAssignment(
            @PathVariable Long laboratoryId,
            @PathVariable Long staffId) {
        laboratoryStaffService.removeStaffAssignment(laboratoryId, staffId);
        return ResponseEntity.noContent().build();
    }
}
