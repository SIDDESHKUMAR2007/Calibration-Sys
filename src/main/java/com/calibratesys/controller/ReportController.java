package com.calibratesys.controller;

import com.calibratesys.dto.*;
import com.calibratesys.entity.enums.*;
import com.calibratesys.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'LAB_STAFF')")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    private ResponseEntity<byte[]> createCsvResponse(byte[] csvBytes, String filenamePrefix) {
        String filename = filenamePrefix + "-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    // =========================================================================
    // 1. CALIBRATION STATUS REPORT
    // =========================================================================

    @GetMapping("/calibration-status")
    public ResponseEntity<PageResponseDto<CalibrationStatusReportDto>> getCalibrationStatusReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) EquipmentStatus equipmentStatus,
            @RequestParam(required = false) CalibrationResult calibrationResult,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<CalibrationStatusReportDto> report = reportService.getCalibrationStatusReport(
                laboratoryId, equipmentId, equipmentStatus, calibrationResult, priority,
                fromDate, toDate, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/calibration-status/export")
    public ResponseEntity<byte[]> exportCalibrationStatusReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) EquipmentStatus equipmentStatus,
            @RequestParam(required = false) CalibrationResult calibrationResult,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportCalibrationStatusReportCsv(
                laboratoryId, equipmentId, equipmentStatus, calibrationResult, priority,
                fromDate, toDate, sortBy, sortDirection
        );
        return createCsvResponse(csv, "calibration-status-report");
    }

    // =========================================================================
    // 2. UPCOMING CALIBRATIONS REPORT
    // =========================================================================

    @GetMapping("/upcoming-calibrations")
    public ResponseEntity<PageResponseDto<UpcomingCalibrationReportDto>> getUpcomingCalibrationsReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<UpcomingCalibrationReportDto> report = reportService.getUpcomingCalibrationsReport(
                laboratoryId, equipmentId, priority, fromDate, toDate, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/upcoming-calibrations/export")
    public ResponseEntity<byte[]> exportUpcomingCalibrationsReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportUpcomingCalibrationsReportCsv(
                laboratoryId, equipmentId, priority, fromDate, toDate, sortBy, sortDirection
        );
        return createCsvResponse(csv, "upcoming-calibrations-report");
    }

    // =========================================================================
    // 3. DUE CALIBRATIONS REPORT
    // =========================================================================

    @GetMapping("/due-calibrations")
    public ResponseEntity<PageResponseDto<DueCalibrationReportDto>> getDueCalibrationsReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<DueCalibrationReportDto> report = reportService.getDueCalibrationsReport(
                laboratoryId, equipmentId, priority, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/due-calibrations/export")
    public ResponseEntity<byte[]> exportDueCalibrationsReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportDueCalibrationsReportCsv(
                laboratoryId, equipmentId, priority, sortBy, sortDirection
        );
        return createCsvResponse(csv, "due-calibrations-report");
    }

    // =========================================================================
    // 4. OVERDUE CALIBRATIONS REPORT
    // =========================================================================

    @GetMapping("/overdue-calibrations")
    public ResponseEntity<PageResponseDto<OverdueCalibrationReportDto>> getOverdueCalibrationsReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<OverdueCalibrationReportDto> report = reportService.getOverdueCalibrationsReport(
                laboratoryId, equipmentId, priority, fromDate, toDate, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/overdue-calibrations/export")
    public ResponseEntity<byte[]> exportOverdueCalibrationsReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) SchedulePriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportOverdueCalibrationsReportCsv(
                laboratoryId, equipmentId, priority, fromDate, toDate, sortBy, sortDirection
        );
        return createCsvResponse(csv, "overdue-calibrations-report");
    }

    // =========================================================================
    // 5. CALIBRATION HISTORY REPORT
    // =========================================================================

    @GetMapping("/calibration-history")
    public ResponseEntity<PageResponseDto<CalibrationHistoryReportDto>> getCalibrationHistoryReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) CalibrationResult calibrationResult,
            @RequestParam(required = false) CalibratedByType calibratorType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<CalibrationHistoryReportDto> report = reportService.getCalibrationHistoryReport(
                laboratoryId, equipmentId, calibrationResult, calibratorType,
                fromDate, toDate, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/calibration-history/export")
    public ResponseEntity<byte[]> exportCalibrationHistoryReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) CalibrationResult calibrationResult,
            @RequestParam(required = false) CalibratedByType calibratorType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportCalibrationHistoryReportCsv(
                laboratoryId, equipmentId, calibrationResult, calibratorType,
                fromDate, toDate, sortBy, sortDirection
        );
        return createCsvResponse(csv, "calibration-history-report");
    }

    // =========================================================================
    // 6. EQUIPMENT INVENTORY REPORT
    // =========================================================================

    @GetMapping("/equipment")
    public ResponseEntity<PageResponseDto<EquipmentReportDto>> getEquipmentReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) EquipmentStatus equipmentStatus,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<EquipmentReportDto> report = reportService.getEquipmentReport(
                laboratoryId, equipmentStatus, category, manufacturer,
                fromDate, toDate, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/equipment/export")
    public ResponseEntity<byte[]> exportEquipmentReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) EquipmentStatus equipmentStatus,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportEquipmentReportCsv(
                laboratoryId, equipmentStatus, category, manufacturer,
                fromDate, toDate, sortBy, sortDirection
        );
        return createCsvResponse(csv, "equipment-report");
    }

    // =========================================================================
    // 7. LABORATORY REPORT
    // =========================================================================

    @GetMapping("/laboratories")
    public ResponseEntity<PageResponseDto<LaboratoryReportDto>> getLaboratoryReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) LabStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<LaboratoryReportDto> report = reportService.getLaboratoryReport(
                laboratoryId, status, department, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/laboratories/export")
    public ResponseEntity<byte[]> exportLaboratoryReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) LabStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportLaboratoryReportCsv(
                laboratoryId, status, department, sortBy, sortDirection
        );
        return createCsvResponse(csv, "laboratory-report");
    }

    // =========================================================================
    // 8. STAFF ASSIGNMENT REPORT
    // =========================================================================

    @GetMapping("/staff-assignments")
    public ResponseEntity<PageResponseDto<StaffAssignmentReportDto>> getStaffAssignmentReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) StaffLabRole roleInLab,
            @RequestParam(required = false) Boolean isPrimaryContact,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<StaffAssignmentReportDto> report = reportService.getStaffAssignmentReport(
                laboratoryId, staffId, roleInLab, isPrimaryContact, status,
                page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/staff-assignments/export")
    public ResponseEntity<byte[]> exportStaffAssignmentReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) StaffLabRole roleInLab,
            @RequestParam(required = false) Boolean isPrimaryContact,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportStaffAssignmentReportCsv(
                laboratoryId, staffId, roleInLab, isPrimaryContact, status,
                sortBy, sortDirection
        );
        return createCsvResponse(csv, "staff-assignment-report");
    }

    // =========================================================================
    // 9. COMPLIANCE ATTENTION REPORT
    // =========================================================================

    @GetMapping("/compliance-attention")
    public ResponseEntity<PageResponseDto<ComplianceAttentionReportDto>> getComplianceAttentionReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageResponseDto<ComplianceAttentionReportDto> report = reportService.getComplianceAttentionReport(
                laboratoryId, category, page, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(report);
    }

    @GetMapping("/compliance-attention/export")
    public ResponseEntity<byte[]> exportComplianceAttentionReport(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] csv = reportService.exportComplianceAttentionReportCsv(
                laboratoryId, category, sortBy, sortDirection
        );
        return createCsvResponse(csv, "compliance-attention-report");
    }
}
