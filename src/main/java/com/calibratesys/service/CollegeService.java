package com.calibratesys.service;

import com.calibratesys.dto.CollegeRequestDto;
import com.calibratesys.dto.CollegeResponseDto;

import java.util.List;

/**
 * Service interface for College management operations.
 */
public interface CollegeService {

    /**
     * Creates a new College.
     */
    CollegeResponseDto createCollege(CollegeRequestDto request);

    /**
     * Retrieves a College by its unique ID.
     */
    CollegeResponseDto getCollegeById(Long id);

    /**
     * Retrieves all Colleges.
     */
    List<CollegeResponseDto> getAllColleges();

    /**
     * Updates an existing College by its ID.
     */
    CollegeResponseDto updateCollege(Long id, CollegeRequestDto request);

    /**
     * Safely deletes a College by its ID, enforcing relationship constraints.
     */
    void deleteCollege(Long id);
}
