package com.calibratesys.service.impl;

import com.calibratesys.dto.CalibrationScheduleRequestDto;
import com.calibratesys.dto.CalibrationScheduleResponseDto;
import com.calibratesys.dto.CalibrationScheduleUpdateRequestDto;
import com.calibratesys.entity.CalibrationSchedule;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.CalibrationRecordRepository;
import com.calibratesys.repository.CalibrationScheduleRepository;
import com.calibratesys.repository.EquipmentRepository;
import com.calibratesys.service.CalibrationScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CalibrationScheduleServiceImpl implements CalibrationScheduleService {

    private final CalibrationScheduleRepository scheduleRepository;
    private final EquipmentRepository equipmentRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;
    private final Clock clock;
    private final int upcomingDays;
    private final com.calibratesys.service.AuditService auditService;
    private final com.calibratesys.security.DataScopeService dataScopeService;

    @Autowired
    public CalibrationScheduleServiceImpl(CalibrationScheduleRepository scheduleRepository,
                                          EquipmentRepository equipmentRepository,
                                          CalibrationRecordRepository calibrationRecordRepository,
                                          Clock clock,
                                          @Value("${app.calibration.upcoming-days:30}") int upcomingDays,
                                          com.calibratesys.service.AuditService auditService,
                                          com.calibratesys.security.DataScopeService dataScopeService) {
        this.scheduleRepository = scheduleRepository;
        this.equipmentRepository = equipmentRepository;
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.clock = clock;
        this.upcomingDays = upcomingDays;
        this.auditService = auditService;
        this.dataScopeService = dataScopeService;
    }

    private LocalDate getToday() {
        return LocalDate.now(clock);
    }

    /**
     * Determines the date-driven schedule status based on current date and configured upcoming window.
     * Preserves terminal statuses (COMPLETED, CANCELLED).
     */
    public ScheduleStatus calculateDateDrivenStatus(LocalDate dueDate, LocalDate today, ScheduleStatus currentStatus) {
        if (currentStatus == ScheduleStatus.COMPLETED || currentStatus == ScheduleStatus.CANCELLED) {
            return currentStatus;
        }

        if (dueDate.isBefore(today)) {
            return ScheduleStatus.OVERDUE;
        } else if (dueDate.isEqual(today)) {
            return ScheduleStatus.DUE;
        } else if (!dueDate.isAfter(today.plusDays(upcomingDays))) {
            return ScheduleStatus.UPCOMING;
        } else {
            return ScheduleStatus.SCHEDULED;
        }
    }

    @Override
    public CalibrationScheduleResponseDto createSchedule(CalibrationScheduleRequestDto requestDto) {
        Equipment equipment = equipmentRepository.findById(requestDto.getEquipmentId())
                .orElseThrow(() -> new ApiException("Equipment not found with ID: " + requestDto.getEquipmentId(), HttpStatus.NOT_FOUND));

        if (requestDto.getDueDate().isBefore(requestDto.getScheduledDate())) {
            throw new ApiException("Due date cannot be before scheduled date", HttpStatus.BAD_REQUEST);
        }

        ScheduleStatus initialStatus = calculateDateDrivenStatus(
                requestDto.getDueDate(),
                getToday(),
                ScheduleStatus.SCHEDULED
        );

        CalibrationSchedule schedule = new CalibrationSchedule(
                equipment,
                requestDto.getScheduledDate(),
                requestDto.getDueDate(),
                initialStatus,
                requestDto.getPriority(),
                requestDto.getRemarks()
        );

        CalibrationSchedule saved = scheduleRepository.save(schedule);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.CALIBRATION_SCHEDULE_CREATED,
                com.calibratesys.entity.enums.AuditEntityType.CALIBRATION_SCHEDULE,
                saved.getId(),
                "Calibration schedule #" + saved.getId() + " for equipment " + equipment.getAssetTag() + " was created.",
                null,
                java.util.Map.of("equipmentId", equipment.getId(), "assetTag", equipment.getAssetTag(), "dueDate", saved.getDueDate().toString(), "status", saved.getStatus().name(), "priority", saved.getPriority().name())
        );

        return mapToResponseDto(saved);
    }

    @Override
    public CalibrationScheduleResponseDto updateSchedule(Long id, CalibrationScheduleUpdateRequestDto updateDto) {
        CalibrationSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration schedule not found with ID: " + id, HttpStatus.NOT_FOUND));

        if (updateDto.getDueDate().isBefore(updateDto.getScheduledDate())) {
            throw new ApiException("Due date cannot be before scheduled date", HttpStatus.BAD_REQUEST);
        }

        LocalDate oldDue = schedule.getDueDate();
        LocalDate oldScheduled = schedule.getScheduledDate();
        String oldStatus = schedule.getStatus().name();
        String oldPriority = schedule.getPriority().name();

        schedule.setScheduledDate(updateDto.getScheduledDate());
        schedule.setDueDate(updateDto.getDueDate());
        schedule.setPriority(updateDto.getPriority());
        schedule.setRemarks(updateDto.getRemarks());

        ScheduleStatus targetStatus = updateDto.getStatus();
        if (targetStatus == ScheduleStatus.COMPLETED || targetStatus == ScheduleStatus.CANCELLED) {
            schedule.setStatus(targetStatus);
        } else {
            schedule.setStatus(calculateDateDrivenStatus(updateDto.getDueDate(), getToday(), targetStatus));
        }

        CalibrationSchedule updated = scheduleRepository.save(schedule);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.CALIBRATION_SCHEDULE_UPDATED,
                com.calibratesys.entity.enums.AuditEntityType.CALIBRATION_SCHEDULE,
                updated.getId(),
                "Calibration schedule #" + updated.getId() + " for equipment " + updated.getEquipment().getAssetTag() + " was updated.",
                java.util.Map.of("scheduledDate", oldScheduled.toString(), "dueDate", oldDue.toString(), "status", oldStatus, "priority", oldPriority),
                java.util.Map.of("scheduledDate", updated.getScheduledDate().toString(), "dueDate", updated.getDueDate().toString(), "status", updated.getStatus().name(), "priority", updated.getPriority().name())
        );

        return mapToResponseDto(updated);
    }

    @Override
    public CalibrationScheduleResponseDto cancelSchedule(Long id) {
        CalibrationSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration schedule not found with ID: " + id, HttpStatus.NOT_FOUND));

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            throw new ApiException("Cannot cancel a completed calibration schedule #" + id, HttpStatus.BAD_REQUEST);
        }

        schedule.setStatus(ScheduleStatus.CANCELLED);
        CalibrationSchedule updated = scheduleRepository.save(schedule);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.CALIBRATION_SCHEDULE_CANCELLED,
                com.calibratesys.entity.enums.AuditEntityType.CALIBRATION_SCHEDULE,
                id,
                "Calibration schedule #" + id + " for equipment " + updated.getEquipment().getAssetTag() + " was cancelled.",
                java.util.Map.of("status", "SCHEDULED"),
                java.util.Map.of("status", "CANCELLED")
        );

        return mapToResponseDto(updated);
    }

    @Override
    public void deleteSchedule(Long id) {
        CalibrationSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration schedule not found with ID: " + id, HttpStatus.NOT_FOUND));

        if (calibrationRecordRepository.existsByCalibrationScheduleId(id)) {
            throw new ApiException("Cannot delete calibration schedule #" + id + " because associated calibration records exist. Cancel the schedule instead to maintain historical integrity.", HttpStatus.CONFLICT);
        }

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            throw new ApiException("Cannot delete completed calibration schedule #" + id + ". Completed schedules must be preserved for audit trail.", HttpStatus.CONFLICT);
        }

        scheduleRepository.delete(schedule);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.CALIBRATION_SCHEDULE_DELETED,
                com.calibratesys.entity.enums.AuditEntityType.CALIBRATION_SCHEDULE,
                id,
                "Calibration schedule #" + id + " was deleted.",
                java.util.Map.of("equipmentId", schedule.getEquipment().getId(), "dueDate", schedule.getDueDate().toString()),
                null
        );
    }

    @Override
    public CalibrationScheduleResponseDto getScheduleById(Long id) {
        CalibrationSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration schedule not found with ID: " + id, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = schedule.getEquipment() != null && schedule.getEquipment().getLaboratory() != null
                ? schedule.getEquipment().getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access calibration schedule in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        // Refresh status if non-terminal
        ScheduleStatus expected = calculateDateDrivenStatus(schedule.getDueDate(), getToday(), schedule.getStatus());
        if (schedule.getStatus() != expected) {
            schedule.setStatus(expected);
            schedule = scheduleRepository.save(schedule);
        }

        return mapToResponseDto(schedule);
    }

    @Override
    public List<CalibrationScheduleResponseDto> getSchedules(Long equipmentId, Long laboratoryId,
                                                             ScheduleStatus status, SchedulePriority priority,
                                                             LocalDate fromDate, LocalDate toDate) {
        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope(laboratoryId);
        if (equipmentId != null) {
            Equipment eq = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new ApiException("Equipment not found with ID: " + equipmentId, HttpStatus.NOT_FOUND));
            Long eqLabId = eq.getLaboratory() != null ? eq.getLaboratory().getId() : null;
            if (eqLabId != null && !scope.isPermitted(eqLabId)) {
                throw new ApiException("Access denied: You do not have permission to access equipment in laboratory ID: " + eqLabId, HttpStatus.FORBIDDEN);
            }
        }
        if (scope.isEmptyScope()) {
            return Collections.emptyList();
        }

        refreshActiveScheduleStatuses();
        List<CalibrationSchedule> schedules = scheduleRepository.findWithFilters(
                equipmentId, laboratoryId, status, priority, fromDate, toDate
        );

        if (scope.laboratoryIds() != null && laboratoryId == null) {
            schedules = schedules.stream()
                    .filter(s -> s.getEquipment() != null && s.getEquipment().getLaboratory() != null && scope.isPermitted(s.getEquipment().getLaboratory().getId()))
                    .collect(Collectors.toList());
        }

        return schedules.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Override
    public List<CalibrationScheduleResponseDto> getSchedulesByEquipmentId(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ApiException("Equipment not found with ID: " + equipmentId, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = equipment.getLaboratory() != null ? equipment.getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access calibration schedules for laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        refreshActiveScheduleStatuses();
        return scheduleRepository.findByEquipmentIdOrderByDueDateAsc(equipmentId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalibrationScheduleResponseDto> getUpcomingSchedules() {
        return getSchedules(null, null, ScheduleStatus.UPCOMING, null, null, null);
    }

    @Override
    public List<CalibrationScheduleResponseDto> getDueSchedules() {
        return getSchedules(null, null, ScheduleStatus.DUE, null, null, null);
    }

    @Override
    public List<CalibrationScheduleResponseDto> getOverdueSchedules() {
        return getSchedules(null, null, ScheduleStatus.OVERDUE, null, null, null);
    }

    @Override
    public void refreshActiveScheduleStatuses() {
        LocalDate today = getToday();
        List<CalibrationSchedule> activeList = scheduleRepository.findByStatusNotIn(
                List.of(ScheduleStatus.COMPLETED, ScheduleStatus.CANCELLED)
        );

        for (CalibrationSchedule s : activeList) {
            ScheduleStatus calculated = calculateDateDrivenStatus(s.getDueDate(), today, s.getStatus());
            if (s.getStatus() != calculated) {
                s.setStatus(calculated);
                scheduleRepository.save(s);
            }
        }
    }

    private static final java.util.Map<String, String> SCHEDULE_SORT_FIELDS = java.util.Map.of(
            "dueDate", "dueDate",
            "scheduledDate", "scheduledDate",
            "status", "status",
            "priority", "priority",
            "createdAt", "createdAt"
    );

    @Override
    @Transactional
    public com.calibratesys.dto.PageResponseDto<CalibrationScheduleResponseDto> searchSchedules(
            Long equipmentId,
            Long laboratoryId,
            String assetTag,
            String status,
            String priority,
            LocalDate scheduledStartDate,
            LocalDate scheduledEndDate,
            LocalDate dueStartDate,
            LocalDate dueEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        if (scheduledStartDate != null && scheduledEndDate != null && scheduledStartDate.isAfter(scheduledEndDate)) {
            throw new ApiException("Scheduled start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }
        if (dueStartDate != null && dueEndDate != null && dueStartDate.isAfter(dueEndDate)) {
            throw new ApiException("Due start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }

        refreshActiveScheduleStatuses();

        ScheduleStatus parsedStatus = null;
        if (org.springframework.util.StringUtils.hasText(status)) {
            try {
                parsedStatus = ScheduleStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid schedule status: " + status, HttpStatus.BAD_REQUEST);
            }
        }

        SchedulePriority parsedPriority = null;
        if (org.springframework.util.StringUtils.hasText(priority)) {
            try {
                parsedPriority = SchedulePriority.valueOf(priority.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid schedule priority: " + priority, HttpStatus.BAD_REQUEST);
            }
        }

        String tag = org.springframework.util.StringUtils.hasText(assetTag) ? assetTag.trim() : null;
        String q = org.springframework.util.StringUtils.hasText(search) ? search.trim() : null;

        org.springframework.data.domain.Sort sort = com.calibratesys.util.SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "dueDate", org.springframework.data.domain.Sort.Direction.ASC, SCHEDULE_SORT_FIELDS
        );

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(validatedPage, validatedSize, sort);

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope(laboratoryId);
        if (scope.isEmptyScope()) {
            return com.calibratesys.dto.PageResponseDto.from(new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0));
        }

        org.springframework.data.domain.Page<CalibrationSchedule> pageResult;
        if (scope.laboratoryIds() != null) {
            pageResult = scheduleRepository.findWithAdvancedFiltersAndLaboratoryIds(
                    scope.laboratoryIds(), equipmentId, laboratoryId, tag, parsedStatus, parsedPriority,
                    scheduledStartDate, scheduledEndDate, dueStartDate, dueEndDate, q, pageable
            );
        } else {
            pageResult = scheduleRepository.findWithAdvancedFilters(
                    equipmentId, laboratoryId, tag, parsedStatus, parsedPriority,
                    scheduledStartDate, scheduledEndDate, dueStartDate, dueEndDate, q, pageable
            );
        }

        return com.calibratesys.dto.PageResponseDto.from(pageResult.map(this::mapToResponseDto));
    }

    private CalibrationScheduleResponseDto mapToResponseDto(CalibrationSchedule s) {
        Equipment e = s.getEquipment();
        Laboratory lab = e != null ? e.getLaboratory() : null;
        College college = lab != null ? lab.getCollege() : null;

        return new CalibrationScheduleResponseDto(
                s.getId(),
                e != null ? e.getId() : null,
                e != null ? e.getName() : null,
                e != null ? e.getAssetTag() : null,
                e != null ? e.getSerialNumber() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getLabCode() : null,
                lab != null ? lab.getName() : null,
                college != null ? college.getId() : null,
                college != null ? college.getCode() : null,
                college != null ? college.getName() : null,
                s.getScheduledDate(),
                s.getDueDate(),
                s.getStatus(),
                s.getPriority(),
                s.getRemarks(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
