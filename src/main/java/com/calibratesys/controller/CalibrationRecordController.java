package com.calibratesys.controller;

import com.calibratesys.dto.CalibrationDocumentResponseDto;
import com.calibratesys.dto.CalibrationRecordRequestDto;
import com.calibratesys.dto.CalibrationRecordResponseDto;
import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;
import com.calibratesys.service.CalibrationDocumentService;
import com.calibratesys.service.CalibrationRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Calibration Records and History.
 * Read operations are accessible to all authenticated users (ADMIN, LAB_STAFF).
 * Creation is restricted to ADMIN role.
 * Records are immutable audit logs and cannot be physically deleted.
 */
@RestController
@RequestMapping("/api/calibration-records")
public class CalibrationRecordController {

    private final CalibrationRecordService recordService;
    private final CalibrationDocumentService documentService;

    @Autowired
    public CalibrationRecordController(CalibrationRecordService recordService,
                                       CalibrationDocumentService documentService) {
        this.recordService = recordService;
        this.documentService = documentService;
    }

    /**
     * List calibration records with optional filtering.
     * Examples:
     *   GET /api/calibration-records
     *   GET /api/calibration-records?equipmentId=5
     *   GET /api/calibration-records?laboratoryId=2
     *   GET /api/calibration-records?result=PASSED
     *   GET /api/calibration-records?calibratedByType=INTERNAL
     *   GET /api/calibration-records?fromDate=2026-01-01&toDate=2026-12-31
     */
    @GetMapping
    public ResponseEntity<?> getRecords(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String calibratedByType,
            @RequestParam(required = false) String calibratorName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate calibrationStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate calibrationEndDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueEndDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        CalibrationResult parsedResult = null;
        if (org.springframework.util.StringUtils.hasText(result)) {
            parsedResult = CalibrationResult.valueOf(result.trim().toUpperCase());
        }
        CalibratedByType parsedType = null;
        if (org.springframework.util.StringUtils.hasText(calibratedByType)) {
            parsedType = CalibratedByType.valueOf(calibratedByType.trim().toUpperCase());
        }

        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            com.calibratesys.util.PaginationValidator.validate(p, s);
            LocalDate effCalStart = calibrationStartDate != null ? calibrationStartDate : fromDate;
            LocalDate effCalEnd = calibrationEndDate != null ? calibrationEndDate : toDate;
            com.calibratesys.dto.PageResponseDto<CalibrationRecordResponseDto> response = recordService.searchRecords(
                    equipmentId, laboratoryId, parsedResult, parsedType, calibratorName,
                    effCalStart, effCalEnd, dueStartDate, dueEndDate, search, sortBy, sortDirection, p, s
            );
            return ResponseEntity.ok(response);
        }

        List<CalibrationRecordResponseDto> records = recordService.getRecords(
                equipmentId, laboratoryId, parsedResult, parsedType, fromDate, toDate
        );
        return ResponseEntity.ok(records);
    }

    /**
     * Retrieve a specific calibration record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CalibrationRecordResponseDto> getRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    /**
     * Record a new calibration event for equipment (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalibrationRecordResponseDto> createRecord(
            @Valid @RequestBody CalibrationRecordRequestDto request) {
        CalibrationRecordResponseDto created = recordService.createRecord(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Retrieve documents / certificates associated with a specific calibration record.
     */
    @GetMapping("/{calibrationRecordId}/documents")
    public ResponseEntity<List<CalibrationDocumentResponseDto>> getRecordDocuments(@PathVariable Long calibrationRecordId) {
        return ResponseEntity.ok(documentService.getDocumentsByCalibrationRecord(calibrationRecordId));
    }
}
