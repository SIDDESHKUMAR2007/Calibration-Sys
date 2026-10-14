package com.calibratesys.service;

import com.calibratesys.dto.LaboratoryStaffRequestDto;
import com.calibratesys.dto.LaboratoryStaffResponseDto;
import com.calibratesys.dto.LaboratoryStaffUpdateDto;

import java.util.List;

/**
 * Service interface for Laboratory-Staff Many-to-Many Assignment Management.
 */
public interface LaboratoryStaffService {

    /**
     * List all staff members assigned to a laboratory.
     */
    List<LaboratoryStaffResponseDto> getStaffByLaboratory(Long laboratoryId);

    /**
     * List all laboratories assigned to a staff member.
     */
    List<LaboratoryStaffResponseDto> getLaboratoriesByStaff(Long staffId);

    /**
     * Assign a staff member to a laboratory with role, primary-contact flag, and status.
     */
    LaboratoryStaffResponseDto assignStaffToLaboratory(Long laboratoryId, LaboratoryStaffRequestDto request);

    /**
     * Update an existing assignment's role, primary-contact flag, or status.
     */
    LaboratoryStaffResponseDto updateStaffAssignment(Long laboratoryId, Long staffId, LaboratoryStaffUpdateDto request);

    /**
     * Remove a staff assignment from a laboratory.
     */
    void removeStaffAssignment(Long laboratoryId, Long staffId);
}
