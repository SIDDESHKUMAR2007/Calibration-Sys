package com.calibratesys.service;

import com.calibratesys.dto.StaffRequestDto;
import com.calibratesys.dto.StaffResponseDto;
import com.calibratesys.dto.StaffUpdateRequestDto;

import java.util.List;

/**
 * Service interface for Staff lifecycle management.
 */
public interface StaffService {

    /**
     * Create a new staff member.
     */
    StaffResponseDto createStaff(StaffRequestDto request);

    /**
     * Retrieve staff details by ID.
     */
    StaffResponseDto getStaffById(Long id);

    /**
     * List all staff with optional status and department filters.
     */
    List<StaffResponseDto> getAllStaff(String status, String department);

    /**
     * Update an existing staff member.
     */
    StaffResponseDto updateStaff(Long id, StaffUpdateRequestDto request);

    /**
     * Delete a staff member with foreign-key and assignment delete-safety checks.
     */
    void deleteStaff(Long id);

    /**
     * Advanced multi-criteria search and filter for staff members.
     */
    List<StaffResponseDto> searchStaff(String status, String department, String systemRole, Long laboratoryId, String roleInLab, String search);

    com.calibratesys.dto.PageResponseDto<StaffResponseDto> searchStaffPaginated(
            String status, String department, String systemRole, Long laboratoryId, String roleInLab, String search,
            String sortBy, String sortDirection, int page, int size
    );
}
