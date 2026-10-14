package com.calibratesys.service.impl;

import com.calibratesys.dto.*;
import com.calibratesys.entity.CalibrationRecord;
import com.calibratesys.entity.CalibrationSchedule;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.LaboratoryStaff;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.EquipmentStatus;
import com.calibratesys.entity.enums.LabStatus;
import com.calibratesys.entity.enums.ScheduleStatus;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.*;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.security.ScopedContext;
import com.calibratesys.service.CalibrationScheduleService;
import com.calibratesys.service.DashboardService;
import com.calibratesys.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final EquipmentRepository equipmentRepository;
    private final CalibrationScheduleRepository calibrationScheduleRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;
    private final CalibrationDocumentRepository calibrationDocumentRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final CollegeRepository collegeRepository;
    private final StaffRepository staffRepository;
    private final NotificationRepository notificationRepository;
    private final CalibrationScheduleService calibrationScheduleService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final com.calibratesys.security.DataScopeService dataScopeService;

    @Autowired
    public DashboardServiceImpl(EquipmentRepository equipmentRepository,
                                CalibrationScheduleRepository calibrationScheduleRepository,
                                CalibrationRecordRepository calibrationRecordRepository,
                                CalibrationDocumentRepository calibrationDocumentRepository,
                                LaboratoryRepository laboratoryRepository,
                                LaboratoryStaffRepository laboratoryStaffRepository,
                                CollegeRepository collegeRepository,
                                StaffRepository staffRepository,
                                NotificationRepository notificationRepository,
                                CalibrationScheduleService calibrationScheduleService,
                                NotificationService notificationService,
                                Clock clock,
                                com.calibratesys.security.DataScopeService dataScopeService) {
        this.equipmentRepository = equipmentRepository;
        this.calibrationScheduleRepository = calibrationScheduleRepository;
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.calibrationDocumentRepository = calibrationDocumentRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.collegeRepository = collegeRepository;
        this.staffRepository = staffRepository;
        this.notificationRepository = notificationRepository;
        this.calibrationScheduleService = calibrationScheduleService;
        this.notificationService = notificationService;
        this.clock = clock;
        this.dataScopeService = dataScopeService;
    }

    private LocalDate getToday() {
        return LocalDate.now(clock);
    }

    private com.calibratesys.security.ScopedContext resolveScope(Long requestedLaboratoryId) {
        return dataScopeService.resolveScope(requestedLaboratoryId);
    }

    @Override
    @Transactional
    public DashboardSummaryDto getSummary(Long laboratoryId) {
        calibrationScheduleService.refreshActiveScheduleStatuses();
        ScopedContext ctx = resolveScope(laboratoryId);

        Long unreadNotifs = notificationRepository.countByRecipientStaffIdAndIsReadFalse(ctx.staff().getId());

        if (ctx.isEmptyScope()) {
            return new DashboardSummaryDto(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, unreadNotifs);
        }

        Long totalColleges;
        Long totalLaboratories;
        Long activeLaboratories;

        if (ctx.laboratoryIds() == null) {
            totalColleges = collegeRepository.count();
            totalLaboratories = laboratoryRepository.count();
            activeLaboratories = laboratoryRepository.countByStatus(LabStatus.ACTIVE);
        } else {
            totalColleges = collegeRepository.countDistinctCollegesByLaboratoryIdIn(ctx.laboratoryIds());
            totalLaboratories = (long) ctx.laboratoryIds().size();
            activeLaboratories = laboratoryRepository.countByStatusAndIdIn(LabStatus.ACTIVE, ctx.laboratoryIds());
        }

        // Equipment status counts
        List<Object[]> eqStatusRows = (ctx.laboratoryIds() == null)
                ? equipmentRepository.countEquipmentByStatus()
                : equipmentRepository.countEquipmentByStatusForLaboratoryIds(ctx.laboratoryIds());

        Map<EquipmentStatus, Long> eqMap = new EnumMap<>(EquipmentStatus.class);
        long totalEquipment = 0L;
        for (Object[] row : eqStatusRows) {
            EquipmentStatus status = (EquipmentStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            eqMap.put(status, count);
            totalEquipment += count;
        }

        Long operational = eqMap.getOrDefault(EquipmentStatus.OPERATIONAL, 0L);
        Long underMaintenance = eqMap.getOrDefault(EquipmentStatus.UNDER_MAINTENANCE, 0L);
        Long calibDue = eqMap.getOrDefault(EquipmentStatus.CALIBRATION_DUE, 0L);
        Long outOfCalib = eqMap.getOrDefault(EquipmentStatus.OUT_OF_CALIBRATION, 0L);
        Long decommissioned = eqMap.getOrDefault(EquipmentStatus.DECOMMISSIONED, 0L);

        // Schedule counts
        List<Object[]> schedStatusRows = (ctx.laboratoryIds() == null)
                ? calibrationScheduleRepository.countSchedulesByStatus()
                : calibrationScheduleRepository.countSchedulesByStatusForLaboratoryIds(ctx.laboratoryIds());

        Map<ScheduleStatus, Long> schedMap = new EnumMap<>(ScheduleStatus.class);
        for (Object[] row : schedStatusRows) {
            ScheduleStatus status = (ScheduleStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            schedMap.put(status, count);
        }

        Long upcoming = schedMap.getOrDefault(ScheduleStatus.UPCOMING, 0L);
        Long due = schedMap.getOrDefault(ScheduleStatus.DUE, 0L);
        Long overdue = schedMap.getOrDefault(ScheduleStatus.OVERDUE, 0L);
        Long completed = schedMap.getOrDefault(ScheduleStatus.COMPLETED, 0L);

        // Calibration record and document counts
        Long totalRecords;
        Long recordsWithDocs;
        if (ctx.laboratoryIds() == null) {
            totalRecords = calibrationRecordRepository.count();
            recordsWithDocs = calibrationDocumentRepository.countDistinctRecordsWithDocuments();
        } else {
            totalRecords = calibrationRecordRepository.countByEquipmentLaboratoryIdIn(ctx.laboratoryIds());
            recordsWithDocs = calibrationDocumentRepository.countDistinctRecordsWithDocumentsForLaboratoryIds(ctx.laboratoryIds());
        }

        return new DashboardSummaryDto(
                totalColleges,
                totalLaboratories,
                activeLaboratories,
                totalEquipment,
                operational,
                underMaintenance,
                calibDue,
                outOfCalib,
                decommissioned,
                upcoming,
                due,
                overdue,
                completed,
                totalRecords,
                recordsWithDocs,
                unreadNotifs
        );
    }

    @Override
    @Transactional
    public CalibrationStatusSummaryDto getCalibrationStatusSummary(Long laboratoryId) {
        calibrationScheduleService.refreshActiveScheduleStatuses();
        ScopedContext ctx = resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return new CalibrationStatusSummaryDto(0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        List<Object[]> rows = (ctx.laboratoryIds() == null)
                ? calibrationScheduleRepository.countSchedulesByStatus()
                : calibrationScheduleRepository.countSchedulesByStatusForLaboratoryIds(ctx.laboratoryIds());

        Map<ScheduleStatus, Long> map = new EnumMap<>(ScheduleStatus.class);
        long total = 0L;
        for (Object[] row : rows) {
            ScheduleStatus status = (ScheduleStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            map.put(status, count);
            total += count;
        }

        return new CalibrationStatusSummaryDto(
                map.getOrDefault(ScheduleStatus.SCHEDULED, 0L),
                map.getOrDefault(ScheduleStatus.UPCOMING, 0L),
                map.getOrDefault(ScheduleStatus.DUE, 0L),
                map.getOrDefault(ScheduleStatus.OVERDUE, 0L),
                map.getOrDefault(ScheduleStatus.COMPLETED, 0L),
                map.getOrDefault(ScheduleStatus.CANCELLED, 0L),
                total
        );
    }

    @Override
    public EquipmentStatusSummaryDto getEquipmentStatusSummary(Long laboratoryId) {
        ScopedContext ctx = resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return new EquipmentStatusSummaryDto(0L, 0L, 0L, 0L, 0L, 0L);
        }

        List<Object[]> rows = (ctx.laboratoryIds() == null)
                ? equipmentRepository.countEquipmentByStatus()
                : equipmentRepository.countEquipmentByStatusForLaboratoryIds(ctx.laboratoryIds());

        Map<EquipmentStatus, Long> map = new EnumMap<>(EquipmentStatus.class);
        long total = 0L;
        for (Object[] row : rows) {
            EquipmentStatus status = (EquipmentStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            map.put(status, count);
            total += count;
        }

        return new EquipmentStatusSummaryDto(
                map.getOrDefault(EquipmentStatus.OPERATIONAL, 0L),
                map.getOrDefault(EquipmentStatus.CALIBRATION_DUE, 0L),
                map.getOrDefault(EquipmentStatus.OUT_OF_CALIBRATION, 0L),
                map.getOrDefault(EquipmentStatus.UNDER_MAINTENANCE, 0L),
                map.getOrDefault(EquipmentStatus.DECOMMISSIONED, 0L),
                total
        );
    }

    @Override
    @Transactional
    public List<DashboardCalibrationItemDto> getUpcomingCalibrations(Long laboratoryId) {
        return getCalibrationItemsByStatus(laboratoryId, ScheduleStatus.UPCOMING);
    }

    @Override
    @Transactional
    public List<DashboardCalibrationItemDto> getDueCalibrations(Long laboratoryId) {
        return getCalibrationItemsByStatus(laboratoryId, ScheduleStatus.DUE);
    }

    @Override
    @Transactional
    public List<DashboardCalibrationItemDto> getOverdueCalibrations(Long laboratoryId) {
        return getCalibrationItemsByStatus(laboratoryId, ScheduleStatus.OVERDUE);
    }

    private List<DashboardCalibrationItemDto> getCalibrationItemsByStatus(Long laboratoryId, ScheduleStatus targetStatus) {
        calibrationScheduleService.refreshActiveScheduleStatuses();
        ScopedContext ctx = resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return Collections.emptyList();
        }

        List<CalibrationSchedule> schedules = (ctx.laboratoryIds() == null)
                ? calibrationScheduleRepository.findByStatusWithEquipmentAndLaboratory(targetStatus)
                : calibrationScheduleRepository.findByStatusAndLaboratoryIdInWithEquipmentAndLaboratory(targetStatus, ctx.laboratoryIds());

        LocalDate today = getToday();

        return schedules.stream()
                .map(s -> mapToDashboardItem(s, today))
                .collect(Collectors.toList());
    }

    private DashboardCalibrationItemDto mapToDashboardItem(CalibrationSchedule s, LocalDate today) {
        Equipment eq = s.getEquipment();
        Laboratory lab = eq != null ? eq.getLaboratory() : null;

        Long daysOverdue = null;
        Long daysUntilDue = null;

        if (today.isAfter(s.getDueDate())) {
            daysOverdue = ChronoUnit.DAYS.between(s.getDueDate(), today);
        } else if (today.isBefore(s.getDueDate())) {
            daysUntilDue = ChronoUnit.DAYS.between(today, s.getDueDate());
        } else {
            daysOverdue = 0L;
            daysUntilDue = 0L;
        }

        String staffName = null;
        String staffRole = null;
        if (lab != null) {
            List<Staff> resolved = notificationService.resolveResponsibleStaff(lab);
            if (!resolved.isEmpty()) {
                Staff primary = resolved.get(0);
                staffName = primary.getFullName();
                staffRole = primary.getDesignation() != null ? primary.getDesignation() : "Staff";
            }
        }

        return new DashboardCalibrationItemDto(
                s.getId(),
                eq != null ? eq.getId() : null,
                eq != null ? eq.getName() : null,
                eq != null ? eq.getAssetTag() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getName() : null,
                lab != null ? lab.getLabCode() : null,
                s.getScheduledDate(),
                s.getDueDate(),
                s.getStatus(),
                s.getPriority(),
                daysOverdue,
                daysUntilDue,
                staffName,
                staffRole
        );
    }

    @Override
    public List<RecentCalibrationDto> getRecentCalibrations(Long laboratoryId, int limit) {
        ScopedContext ctx = resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return Collections.emptyList();
        }

        int pageSize = Math.max(1, Math.min(limit, 50));
        PageRequest pageRequest = PageRequest.of(0, pageSize);

        List<CalibrationRecord> records = (ctx.laboratoryIds() == null)
                ? calibrationRecordRepository.findRecentRecords(pageRequest)
                : calibrationRecordRepository.findRecentRecordsForLaboratoryIds(ctx.laboratoryIds(), pageRequest);

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recordIds = records.stream().map(CalibrationRecord::getId).toList();
        List<Object[]> docCountRows = calibrationDocumentRepository.countDocumentsPerRecordId(recordIds);

        Map<Long, Integer> docCountMap = new HashMap<>();
        for (Object[] row : docCountRows) {
            Long recId = ((Number) row[0]).longValue();
            int count = ((Number) row[1]).intValue();
            docCountMap.put(recId, count);
        }

        return records.stream().map(r -> {
            Equipment eq = r.getEquipment();
            Laboratory lab = eq != null ? eq.getLaboratory() : null;
            int docCount = docCountMap.getOrDefault(r.getId(), 0);

            return new RecentCalibrationDto(
                    r.getId(),
                    eq != null ? eq.getId() : null,
                    eq != null ? eq.getName() : null,
                    eq != null ? eq.getAssetTag() : null,
                    lab != null ? lab.getId() : null,
                    lab != null ? lab.getName() : null,
                    r.getCalibrationDate(),
                    r.getNextDueDate(),
                    r.getResult(),
                    r.getCalibratedByType(),
                    r.getCalibratorName(),
                    docCount > 0,
                    docCount
            );
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<LaboratoryDashboardSummaryDto> getLaboratorySummaries(Long laboratoryId, boolean overdueOnly) {
        calibrationScheduleService.refreshActiveScheduleStatuses();
        ScopedContext ctx = resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return Collections.emptyList();
        }

        List<Laboratory> laboratories = (ctx.laboratoryIds() == null)
                ? laboratoryRepository.findAll()
                : laboratoryRepository.findByIdIn(ctx.laboratoryIds());

        if (laboratories.isEmpty()) {
            return Collections.emptyList();
        }

        // Equipment count maps
        List<Object[]> totalEqRows = (ctx.laboratoryIds() == null)
                ? equipmentRepository.countEquipmentGroupedByLaboratory()
                : equipmentRepository.countEquipmentGroupedByLaboratoryForLaboratoryIds(ctx.laboratoryIds());
        Map<Long, Long> totalEqMap = new HashMap<>();
        for (Object[] row : totalEqRows) {
            totalEqMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        List<Object[]> opEqRows = (ctx.laboratoryIds() == null)
                ? equipmentRepository.countEquipmentByStatusGroupedByLaboratory(EquipmentStatus.OPERATIONAL)
                : equipmentRepository.countEquipmentByStatusGroupedByLaboratoryForLaboratoryIds(ctx.laboratoryIds(), EquipmentStatus.OPERATIONAL);
        Map<Long, Long> opEqMap = new HashMap<>();
        for (Object[] row : opEqRows) {
            opEqMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        // Schedules per lab and status
        List<Object[]> schedRows = (ctx.laboratoryIds() == null)
                ? calibrationScheduleRepository.countSchedulesPerLaboratoryAndStatus()
                : calibrationScheduleRepository.countSchedulesPerLaboratoryAndStatusForLaboratoryIds(ctx.laboratoryIds());
        Map<String, Long> schedMap = new HashMap<>();
        for (Object[] row : schedRows) {
            Long labId = ((Number) row[0]).longValue();
            ScheduleStatus status = (ScheduleStatus) row[1];
            Long count = ((Number) row[2]).longValue();
            schedMap.put(labId + "_" + status.name(), count);
        }

        List<LaboratoryDashboardSummaryDto> list = new ArrayList<>();
        for (Laboratory lab : laboratories) {
            Long totalEquipment = totalEqMap.getOrDefault(lab.getId(), 0L);
            Long operationalEquipment = opEqMap.getOrDefault(lab.getId(), 0L);
            Long upcomingCount = schedMap.getOrDefault(lab.getId() + "_" + ScheduleStatus.UPCOMING.name(), 0L);
            Long dueCount = schedMap.getOrDefault(lab.getId() + "_" + ScheduleStatus.DUE.name(), 0L);
            Long overdueCount = schedMap.getOrDefault(lab.getId() + "_" + ScheduleStatus.OVERDUE.name(), 0L);

            boolean hasOverdue = overdueCount > 0;

            if (overdueOnly && !hasOverdue) {
                continue;
            }

            list.add(new LaboratoryDashboardSummaryDto(
                    lab.getId(),
                    lab.getName(),
                    lab.getLabCode(),
                    lab.getCollege() != null ? lab.getCollege().getName() : null,
                    lab.getStatus(),
                    totalEquipment,
                    operationalEquipment,
                    upcomingCount,
                    dueCount,
                    overdueCount,
                    hasOverdue
            ));
        }

        if (overdueOnly) {
            list.sort(Comparator.comparing(LaboratoryDashboardSummaryDto::getOverdueCount).reversed());
        } else {
            list.sort(Comparator.comparing(LaboratoryDashboardSummaryDto::getLaboratoryName));
        }

        return list;
    }

    @Override
    @Transactional
    public DashboardResponseDto getDashboard(Long laboratoryId) {
        DashboardSummaryDto summary = getSummary(laboratoryId);
        CalibrationStatusSummaryDto calibStatus = getCalibrationStatusSummary(laboratoryId);
        EquipmentStatusSummaryDto eqStatus = getEquipmentStatusSummary(laboratoryId);
        List<DashboardCalibrationItemDto> upcoming = getUpcomingCalibrations(laboratoryId);
        List<DashboardCalibrationItemDto> due = getDueCalibrations(laboratoryId);
        List<DashboardCalibrationItemDto> overdue = getOverdueCalibrations(laboratoryId);
        List<RecentCalibrationDto> recent = getRecentCalibrations(laboratoryId, 10);
        List<LaboratoryDashboardSummaryDto> labs = getLaboratorySummaries(laboratoryId, false);

        List<NotificationResponseDto> unreadAlerts = notificationService.getCurrentUserUnreadNotifications()
                .stream().limit(5).collect(Collectors.toList());

        Long unreadCount = summary.getUnreadNotificationsCount();

        return new DashboardResponseDto(
                summary,
                calibStatus,
                eqStatus,
                upcoming,
                due,
                overdue,
                recent,
                labs,
                unreadAlerts,
                unreadCount
        );
    }
}
