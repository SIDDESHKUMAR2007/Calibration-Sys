package com.calibratesys.controller;

import com.calibratesys.dto.CalibrationDocumentResponseDto;
import com.calibratesys.dto.CalibrationRecordResponseDto;
import com.calibratesys.dto.CalibrationScheduleResponseDto;
import com.calibratesys.dto.EquipmentRequestDto;
import com.calibratesys.dto.EquipmentResponseDto;
import com.calibratesys.service.CalibrationRecordService;
import com.calibratesys.service.CalibrationScheduleService;
import com.calibratesys.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Laboratory Equipment inventory operations.
 * Read operations are accessible to all authenticated users (ADMIN, LAB_STAFF).
 * Write and delete operations are restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final CalibrationScheduleService calibrationScheduleService;
    private final CalibrationRecordService calibrationRecordService;
    private final com.calibratesys.service.CalibrationDocumentService calibrationDocumentService;

    @Autowired
    public EquipmentController(EquipmentService equipmentService,
                               CalibrationScheduleService calibrationScheduleService,
                               CalibrationRecordService calibrationRecordService,
                               com.calibratesys.service.CalibrationDocumentService calibrationDocumentService) {
        this.equipmentService = equipmentService;
        this.calibrationScheduleService = calibrationScheduleService;
        this.calibrationRecordService = calibrationRecordService;
        this.calibrationDocumentService = calibrationDocumentService;
    }

    /**
     * List equipment with optional filtering by laboratoryId, status, and search query.
     * Examples:
     *   GET /api/equipment
     *   GET /api/equipment?laboratoryId=1
     *   GET /api/equipment?status=OPERATIONAL
     *   GET /api/equipment?laboratoryId=1&status=OPERATIONAL
     *   GET /api/equipment?search=Centrifuge
     */
    @GetMapping
    public ResponseEntity<?> getEquipment(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String modelNumber,
            @RequestParam(required = false) Integer calibrationFrequencyDays,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate purchaseStartDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate purchaseEndDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate installationStartDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate installationEndDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            com.calibratesys.util.PaginationValidator.validate(p, s);
            com.calibratesys.dto.PageResponseDto<EquipmentResponseDto> result = equipmentService.searchEquipment(
                    laboratoryId, status, manufacturer, modelNumber, calibrationFrequencyDays,
                    purchaseStartDate, purchaseEndDate, installationStartDate, installationEndDate,
                    search, sortBy, sortDirection, p, s
            );
            return ResponseEntity.ok(result);
        }
        List<EquipmentResponseDto> equipment = equipmentService.getEquipment(laboratoryId, status, search);
        return ResponseEntity.ok(equipment);
    }

    /**
     * Retrieve a specific equipment item by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponseDto> getEquipmentById(@PathVariable Long id) {
        EquipmentResponseDto equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(equipment);
    }

    /**
     * Retrieve calibration schedules for a specific equipment item.
     */
    @GetMapping("/{equipmentId}/calibration-schedules")
    public ResponseEntity<List<CalibrationScheduleResponseDto>> getEquipmentCalibrationSchedules(@PathVariable Long equipmentId) {
        List<CalibrationScheduleResponseDto> schedules = calibrationScheduleService.getSchedulesByEquipmentId(equipmentId);
        return ResponseEntity.ok(schedules);
    }

    /**
     * Retrieve calibration records / history for a specific equipment item.
     */
    @GetMapping("/{equipmentId}/calibration-records")
    public ResponseEntity<List<CalibrationRecordResponseDto>> getEquipmentCalibrationRecords(@PathVariable Long equipmentId) {
        List<CalibrationRecordResponseDto> records = calibrationRecordService.getRecordsByEquipmentId(equipmentId);
        return ResponseEntity.ok(records);
    }

    /**
     * Retrieve calibration documents / certificates history for a specific equipment item.
     */
    @GetMapping("/{equipmentId}/calibration-documents")
    public ResponseEntity<List<CalibrationDocumentResponseDto>> getEquipmentCalibrationDocuments(@PathVariable Long equipmentId) {
        List<CalibrationDocumentResponseDto> documents = calibrationDocumentService.getDocumentsByEquipment(equipmentId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Register a new equipment item under an existing laboratory (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentResponseDto> createEquipment(@Valid @RequestBody EquipmentRequestDto request) {
        EquipmentResponseDto created = equipmentService.createEquipment(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing equipment item by ID (ADMIN only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentResponseDto> updateEquipment(@PathVariable Long id,
                                                                @Valid @RequestBody EquipmentRequestDto request) {
        EquipmentResponseDto updated = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an equipment item by ID (ADMIN only, blocked if calibration records/schedules exist).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }
}
