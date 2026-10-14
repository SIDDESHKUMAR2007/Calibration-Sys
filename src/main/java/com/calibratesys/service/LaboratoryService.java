package com.calibratesys.service;

import com.calibratesys.dto.LaboratoryRequestDto;
import com.calibratesys.dto.LaboratoryResponseDto;

import java.util.List;

/**
 * Service interface for Laboratory management operations.
 */
public interface LaboratoryService {

    /**
     * Creates a new Laboratory under an existing College.
     */
    LaboratoryResponseDto createLaboratory(LaboratoryRequestDto request);

    /**
     * Retrieves a Laboratory by its unique ID.
     */
    LaboratoryResponseDto getLaboratoryById(Long id);

    /**
     * Retrieves Laboratories with optional filtering by collegeId and/or status.
     */
    List<LaboratoryResponseDto> getLaboratories(Long collegeId, String status);

    /**
     * Updates an existing Laboratory by its ID.
     */
    LaboratoryResponseDto updateLaboratory(Long id, LaboratoryRequestDto request);

    /**
     * Safely deletes a Laboratory by its ID, enforcing relationship constraints.
     */
    void deleteLaboratory(Long id);

    /**
     * Advanced search, filtering, and scoped retrieval of laboratories.
     */
    List<LaboratoryResponseDto> searchLaboratories(Long collegeId, String status, String department, String search);

    com.calibratesys.dto.PageResponseDto<LaboratoryResponseDto> searchLaboratoriesPaginated(
            Long collegeId, String status, String department, String search,
            String sortBy, String sortDirection, int page, int size
    );
}
