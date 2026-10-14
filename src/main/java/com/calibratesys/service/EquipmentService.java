package com.calibratesys.service;

import com.calibratesys.dto.EquipmentRequestDto;
import com.calibratesys.dto.EquipmentResponseDto;
import com.calibratesys.dto.PageResponseDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Laboratory Equipment lifecycle and inventory management.
 */
public interface EquipmentService {

    /**
     * Register a new equipment item under a specific laboratory.
     */
    EquipmentResponseDto createEquipment(EquipmentRequestDto request);

    /**
     * Retrieve equipment details by ID.
     */
    EquipmentResponseDto getEquipmentById(Long id);

    /**
     * List equipment with optional filtering by laboratory, status, and search query.
     */
    List<EquipmentResponseDto> getEquipment(Long laboratoryId, String status, String search);

    /**
     * Advanced paginated, filtered, and sorted search for equipment adhering to data scope rules.
     */
    PageResponseDto<EquipmentResponseDto> searchEquipment(
            Long laboratoryId,
            String status,
            String manufacturer,
            String modelNumber,
            Integer calibrationFrequencyDays,
            LocalDate purchaseStartDate,
            LocalDate purchaseEndDate,
            LocalDate installationStartDate,
            LocalDate installationEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    );

    /**
     * List all equipment belonging to a specific laboratory.
     */
    List<EquipmentResponseDto> getEquipmentByLaboratory(Long laboratoryId);

    /**
     * Update an existing equipment item.
     */
    EquipmentResponseDto updateEquipment(Long id, EquipmentRequestDto request);

    /**
     * Delete an equipment item with historical data safety checks.
     */
    void deleteEquipment(Long id);
}
