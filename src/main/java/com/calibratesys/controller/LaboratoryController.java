package com.calibratesys.controller;

import com.calibratesys.dto.LaboratoryRequestDto;
import com.calibratesys.dto.LaboratoryResponseDto;
import com.calibratesys.service.LaboratoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Laboratory management operations.
 * Write operations (create, update, delete) are restricted to ADMIN role.
 * Read operations are accessible to all authenticated users.
 */
@RestController
@RequestMapping("/api/laboratories")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;
    private final com.calibratesys.service.EquipmentService equipmentService;

    @Autowired
    public LaboratoryController(LaboratoryService laboratoryService,
                                com.calibratesys.service.EquipmentService equipmentService) {
        this.laboratoryService = laboratoryService;
        this.equipmentService = equipmentService;
    }

    /**
     * List laboratories with optional filtering by collegeId and/or status.
     * Examples:
     *   GET /api/laboratories
     *   GET /api/laboratories?collegeId=1
     *   GET /api/laboratories?status=ACTIVE
     *   GET /api/laboratories?collegeId=1&status=ACTIVE
     */
    @GetMapping
    public ResponseEntity<?> getLaboratories(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            com.calibratesys.util.PaginationValidator.validate(p, s);
            return ResponseEntity.ok(laboratoryService.searchLaboratoriesPaginated(collegeId, status, department, search, sortBy, sortDirection, p, s));
        }
        if (department != null || search != null) {
            return ResponseEntity.ok(laboratoryService.searchLaboratories(collegeId, status, department, search));
        }
        List<LaboratoryResponseDto> laboratories = laboratoryService.getLaboratories(collegeId, status);
        return ResponseEntity.ok(laboratories);
    }

    /**
     * Retrieve a specific laboratory by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LaboratoryResponseDto> getLaboratoryById(@PathVariable Long id) {
        LaboratoryResponseDto laboratory = laboratoryService.getLaboratoryById(id);
        return ResponseEntity.ok(laboratory);
    }

    /**
     * Create a new laboratory under an existing college (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LaboratoryResponseDto> createLaboratory(@Valid @RequestBody LaboratoryRequestDto request) {
        LaboratoryResponseDto created = laboratoryService.createLaboratory(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing laboratory by ID (ADMIN only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LaboratoryResponseDto> updateLaboratory(@PathVariable Long id,
                                                                  @Valid @RequestBody LaboratoryRequestDto request) {
        LaboratoryResponseDto updated = laboratoryService.updateLaboratory(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a laboratory by ID (ADMIN only).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLaboratory(@PathVariable Long id) {
        laboratoryService.deleteLaboratory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieve all equipment registered under a specific laboratory.
     */
    @GetMapping("/{laboratoryId}/equipment")
    public ResponseEntity<List<com.calibratesys.dto.EquipmentResponseDto>> getEquipmentByLaboratory(@PathVariable Long laboratoryId) {
        List<com.calibratesys.dto.EquipmentResponseDto> equipmentList = equipmentService.getEquipmentByLaboratory(laboratoryId);
        return ResponseEntity.ok(equipmentList);
    }
}
