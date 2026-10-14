package com.calibratesys.controller;

import com.calibratesys.dto.CalibrationRecordResponseDto;
import com.calibratesys.dto.CalibrationScheduleRequestDto;
import com.calibratesys.dto.CalibrationScheduleResponseDto;
import com.calibratesys.dto.CalibrationScheduleUpdateRequestDto;
import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;
import com.calibratesys.service.CalibrationRecordService;
import com.calibratesys.service.CalibrationScheduleService;
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
 * REST controller for Calibration Scheduling and Due-Date management.
 * Read operations are accessible to all authenticated users (ADMIN, LAB_STAFF).
 * Write, update, cancel, and delete operations are restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/calibration-schedules")
public class CalibrationScheduleController {

    private final CalibrationScheduleService scheduleService;
    private final CalibrationRecordService recordService;

    @Autowired
    public CalibrationScheduleController(CalibrationScheduleService scheduleService,
                                         CalibrationRecordService recordService) {
        this.scheduleService = scheduleService;
        this.recordService = recordService;
    }

    /**
     * List calibration schedules with optional filters.
     * Examples:
     *   GET /api/calibration-schedules
     *   GET /api/calibration-schedules?equipmentId=1
     *   GET /api/calibration-schedules?laboratoryId=1
     *   GET /api/calibration-schedules?status=OVERDUE
     *   GET /api/calibration-schedules?priority=HIGH
     *   GET /api/calibration-schedules?fromDate=2026-10-01&toDate=2026-10-31
     */
    @GetMapping
    public ResponseEntity<?> getSchedules(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String assetTag,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledEndDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueEndDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            com.calibratesys.util.PaginationValidator.validate(p, s);
            LocalDate effDueStart = dueStartDate != null ? dueStartDate : fromDate;
            LocalDate effDueEnd = dueEndDate != null ? dueEndDate : toDate;
            com.calibratesys.dto.PageResponseDto<CalibrationScheduleResponseDto> result = scheduleService.searchSchedules(
                    equipmentId, laboratoryId, assetTag, status, priority,
                    scheduledStartDate, scheduledEndDate, effDueStart, effDueEnd,
                    search, sortBy, sortDirection, p, s
            );
            return ResponseEntity.ok(result);
        }
        ScheduleStatus parsedStatus = null;
        if (org.springframework.util.StringUtils.hasText(status)) {
            parsedStatus = ScheduleStatus.valueOf(status.trim().toUpperCase());
        }
        SchedulePriority parsedPriority = null;
        if (org.springframework.util.StringUtils.hasText(priority)) {
            parsedPriority = SchedulePriority.valueOf(priority.trim().toUpperCase());
        }
        List<CalibrationScheduleResponseDto> schedules = scheduleService.getSchedules(
                equipmentId, laboratoryId, parsedStatus, parsedPriority, fromDate, toDate
        );
        return ResponseEntity.ok(schedules);
    }

    /**
     * Retrieve upcoming calibration schedules.
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<CalibrationScheduleResponseDto>> getUpcomingSchedules() {
        return ResponseEntity.ok(scheduleService.getUpcomingSchedules());
    }

    /**
     * Retrieve calibration schedules due today.
     */
    @GetMapping("/due")
    public ResponseEntity<List<CalibrationScheduleResponseDto>> getDueSchedules() {
        return ResponseEntity.ok(scheduleService.getDueSchedules());
    }

    /**
     * Retrieve overdue calibration schedules.
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<CalibrationScheduleResponseDto>> getOverdueSchedules() {
        return ResponseEntity.ok(scheduleService.getOverdueSchedules());
    }

    /**
     * Retrieve a specific calibration schedule by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CalibrationScheduleResponseDto> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    /**
     * Retrieve calibration records associated with a specific schedule.
     */
    @GetMapping("/{scheduleId}/calibration-records")
    public ResponseEntity<List<CalibrationRecordResponseDto>> getScheduleCalibrationRecords(@PathVariable Long scheduleId) {
        List<CalibrationRecordResponseDto> records = recordService.getRecordsByScheduleId(scheduleId);
        return ResponseEntity.ok(records);
    }

    /**
     * Create a new calibration schedule for an equipment item (ADMIN only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalibrationScheduleResponseDto> createSchedule(
            @Valid @RequestBody CalibrationScheduleRequestDto request) {
        CalibrationScheduleResponseDto created = scheduleService.createSchedule(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Update an existing calibration schedule (ADMIN only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalibrationScheduleResponseDto> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody CalibrationScheduleUpdateRequestDto request) {
        CalibrationScheduleResponseDto updated = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Cancel a calibration schedule (ADMIN only).
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalibrationScheduleResponseDto> cancelSchedule(@PathVariable Long id) {
        CalibrationScheduleResponseDto cancelled = scheduleService.cancelSchedule(id);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Delete a calibration schedule (ADMIN only, blocked with 409 if historical records exist).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
