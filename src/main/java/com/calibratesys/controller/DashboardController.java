package com.calibratesys.controller;

import com.calibratesys.dto.*;
import com.calibratesys.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'LAB_STAFF')")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Unified aggregated dashboard payload for efficient single-roundtrip page rendering.
     */
    @GetMapping
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @RequestParam(required = false) Long laboratoryId) {
        DashboardResponseDto dashboard = dashboardService.getDashboard(laboratoryId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Summary operational metrics across colleges, laboratories, equipment, and schedules.
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @RequestParam(required = false) Long laboratoryId) {
        DashboardSummaryDto summary = dashboardService.getSummary(laboratoryId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Calibration schedule status breakdown.
     */
    @GetMapping("/calibration-status")
    public ResponseEntity<CalibrationStatusSummaryDto> getCalibrationStatus(
            @RequestParam(required = false) Long laboratoryId) {
        CalibrationStatusSummaryDto statusSummary = dashboardService.getCalibrationStatusSummary(laboratoryId);
        return ResponseEntity.ok(statusSummary);
    }

    /**
     * Equipment status breakdown.
     */
    @GetMapping("/equipment-status")
    public ResponseEntity<EquipmentStatusSummaryDto> getEquipmentStatus(
            @RequestParam(required = false) Long laboratoryId) {
        EquipmentStatusSummaryDto equipmentStatus = dashboardService.getEquipmentStatusSummary(laboratoryId);
        return ResponseEntity.ok(equipmentStatus);
    }

    /**
     * Upcoming calibration schedules within the configured window.
     */
    @GetMapping("/upcoming-calibrations")
    public ResponseEntity<List<DashboardCalibrationItemDto>> getUpcomingCalibrations(
            @RequestParam(required = false) Long laboratoryId) {
        List<DashboardCalibrationItemDto> upcoming = dashboardService.getUpcomingCalibrations(laboratoryId);
        return ResponseEntity.ok(upcoming);
    }

    /**
     * Calibrations due today.
     */
    @GetMapping("/due-calibrations")
    public ResponseEntity<List<DashboardCalibrationItemDto>> getDueCalibrations(
            @RequestParam(required = false) Long laboratoryId) {
        List<DashboardCalibrationItemDto> due = dashboardService.getDueCalibrations(laboratoryId);
        return ResponseEntity.ok(due);
    }

    /**
     * Overdue calibrations requiring immediate attention.
     */
    @GetMapping("/overdue-calibrations")
    public ResponseEntity<List<DashboardCalibrationItemDto>> getOverdueCalibrations(
            @RequestParam(required = false) Long laboratoryId) {
        List<DashboardCalibrationItemDto> overdue = dashboardService.getOverdueCalibrations(laboratoryId);
        return ResponseEntity.ok(overdue);
    }

    /**
     * Recent calibration records and certificate presence indicator.
     */
    @GetMapping("/recent-calibrations")
    public ResponseEntity<List<RecentCalibrationDto>> getRecentCalibrations(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        List<RecentCalibrationDto> recent = dashboardService.getRecentCalibrations(laboratoryId, limit);
        return ResponseEntity.ok(recent);
    }

    /**
     * Laboratory-level compliance and equipment summary.
     */
    @GetMapping("/laboratories")
    public ResponseEntity<List<LaboratoryDashboardSummaryDto>> getLaboratories(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false, defaultValue = "false") boolean overdueOnly) {
        List<LaboratoryDashboardSummaryDto> labs = dashboardService.getLaboratorySummaries(laboratoryId, overdueOnly);
        return ResponseEntity.ok(labs);
    }

    /**
     * Dedicated overdue alerts view for laboratories with overdue equipment.
     */
    @GetMapping("/laboratories/overdue")
    public ResponseEntity<List<LaboratoryDashboardSummaryDto>> getOverdueLaboratories(
            @RequestParam(required = false) Long laboratoryId) {
        List<LaboratoryDashboardSummaryDto> overdueLabs = dashboardService.getLaboratorySummaries(laboratoryId, true);
        return ResponseEntity.ok(overdueLabs);
    }
}
