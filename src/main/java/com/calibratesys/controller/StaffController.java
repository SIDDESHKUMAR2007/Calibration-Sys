package com.calibratesys.controller;

import com.calibratesys.dto.LaboratoryStaffResponseDto;
import com.calibratesys.dto.StaffRequestDto;
import com.calibratesys.dto.StaffResponseDto;
import com.calibratesys.dto.StaffUpdateRequestDto;
import com.calibratesys.service.LaboratoryStaffService;
import com.calibratesys.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Staff Management.
 * Read operations are permitted for all authenticated users.
 * Write and delete operations are restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;
    private final LaboratoryStaffService laboratoryStaffService;

    @Autowired
    public StaffController(StaffService staffService,
                           LaboratoryStaffService laboratoryStaffService) {
        this.staffService = staffService;
        this.laboratoryStaffService = laboratoryStaffService;
    }

    /**
     * List all staff members with optional status and department filters.
     * Examples:
     *   GET /api/staff
     *   GET /api/staff?status=ACTIVE
     *   GET /api/staff?department=Computer%20Science
     */
    @GetMapping
    public ResponseEntity<?> getAllStaff(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String systemRole,
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String roleInLab,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            com.calibratesys.util.PaginationValidator.validate(p, s);
            return ResponseEntity.ok(staffService.searchStaffPaginated(status, department, systemRole, laboratoryId, roleInLab, search, sortBy, sortDirection, p, s));
        }
        if (systemRole != null || laboratoryId != null || roleInLab != null || search != null) {
            return ResponseEntity.ok(staffService.searchStaff(status, department, systemRole, laboratoryId, roleInLab, search));
        }
        List<StaffResponseDto> staffList = staffService.getAllStaff(status, department);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Retrieve staff details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StaffResponseDto> getStaffById(@PathVariable Long id) {
        StaffResponseDto staff = staffService.getStaffById(id);
        return ResponseEntity.ok(staff);
    }

    /**
     * Create a new staff member (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponseDto> createStaff(@Valid @RequestBody StaffRequestDto request) {
        StaffResponseDto created = staffService.createStaff(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing staff member (ADMIN only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponseDto> updateStaff(@PathVariable Long id,
                                                        @Valid @RequestBody StaffUpdateRequestDto request) {
        StaffResponseDto updated = staffService.updateStaff(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a staff member by ID (ADMIN only, blocked if laboratory assignments exist).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all laboratories assigned to a specific staff member.
     */
    @GetMapping("/{staffId}/laboratories")
    public ResponseEntity<List<LaboratoryStaffResponseDto>> getStaffLaboratories(@PathVariable Long staffId) {
        List<LaboratoryStaffResponseDto> assignments = laboratoryStaffService.getLaboratoriesByStaff(staffId);
        return ResponseEntity.ok(assignments);
    }
}
