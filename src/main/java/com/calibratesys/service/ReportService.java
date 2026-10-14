package com.calibratesys.service;

import com.calibratesys.dto.*;
import com.calibratesys.entity.enums.*;

import java.time.LocalDate;

public interface ReportService {

    // 1. Calibration Status Report
    PageResponseDto<CalibrationStatusReportDto> getCalibrationStatusReport(
            Long laboratoryId, Long equipmentId, EquipmentStatus equipmentStatus,
            CalibrationResult calibrationResult, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportCalibrationStatusReportCsv(
            Long laboratoryId, Long equipmentId, EquipmentStatus equipmentStatus,
            CalibrationResult calibrationResult, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection
    );

    // 2. Upcoming Calibrations Report
    PageResponseDto<UpcomingCalibrationReportDto> getUpcomingCalibrationsReport(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportUpcomingCalibrationsReportCsv(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection
    );

    // 3. Due Calibrations Report
    PageResponseDto<DueCalibrationReportDto> getDueCalibrationsReport(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportDueCalibrationsReportCsv(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            String sortBy, String sortDirection
    );

    // 4. Overdue Calibrations Report
    PageResponseDto<OverdueCalibrationReportDto> getOverdueCalibrationsReport(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportOverdueCalibrationsReportCsv(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection
    );

    // 5. Calibration History Report
    PageResponseDto<CalibrationHistoryReportDto> getCalibrationHistoryReport(
            Long laboratoryId, Long equipmentId, CalibrationResult calibrationResult,
            CalibratedByType calibratorType, LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportCalibrationHistoryReportCsv(
            Long laboratoryId, Long equipmentId, CalibrationResult calibrationResult,
            CalibratedByType calibratorType, LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection
    );

    // 6. Equipment Inventory Report
    PageResponseDto<EquipmentReportDto> getEquipmentReport(
            Long laboratoryId, EquipmentStatus equipmentStatus, String category,
            String manufacturer, LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportEquipmentReportCsv(
            Long laboratoryId, EquipmentStatus equipmentStatus, String category,
            String manufacturer, LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection
    );

    // 7. Laboratory Report
    PageResponseDto<LaboratoryReportDto> getLaboratoryReport(
            Long laboratoryId, LabStatus status, String department,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportLaboratoryReportCsv(
            Long laboratoryId, LabStatus status, String department,
            String sortBy, String sortDirection
    );

    // 8. Staff Assignment Report
    PageResponseDto<StaffAssignmentReportDto> getStaffAssignmentReport(
            Long laboratoryId, Long staffId, StaffLabRole roleInLab,
            Boolean isPrimaryContact, String status,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportStaffAssignmentReportCsv(
            Long laboratoryId, Long staffId, StaffLabRole roleInLab,
            Boolean isPrimaryContact, String status,
            String sortBy, String sortDirection
    );

    // 9. Compliance Attention Report
    PageResponseDto<ComplianceAttentionReportDto> getComplianceAttentionReport(
            Long laboratoryId, String category,
            int page, int size, String sortBy, String sortDirection
    );

    byte[] exportComplianceAttentionReportCsv(
            Long laboratoryId, String category,
            String sortBy, String sortDirection
    );
}
