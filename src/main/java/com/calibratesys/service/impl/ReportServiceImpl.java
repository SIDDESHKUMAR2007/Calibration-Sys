package com.calibratesys.service.impl;

import com.calibratesys.dto.*;
import com.calibratesys.entity.*;
import com.calibratesys.entity.enums.*;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.*;
import com.calibratesys.security.DataScopeService;
import com.calibratesys.security.ScopedContext;
import com.calibratesys.service.AuditService;
import com.calibratesys.service.CalibrationScheduleService;
import com.calibratesys.service.ReportService;
import com.calibratesys.util.CsvExportUtil;
import com.calibratesys.util.SortValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final EquipmentRepository equipmentRepository;
    private final CalibrationScheduleRepository calibrationScheduleRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;
    private final CalibrationDocumentRepository calibrationDocumentRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final DataScopeService dataScopeService;
    private final AuditService auditService;
    private final CalibrationScheduleService calibrationScheduleService;
    private final Clock clock;

    private static final Map<String, String> CALIBRATION_STATUS_SORT_FIELDS = Map.of(
            "equipmentName", "name",
            "assetTag", "assetTag",
            "laboratory", "laboratory.name",
            "status", "status",
            "id", "id"
    );

    private static final Map<String, String> SCHEDULE_SORT_FIELDS = Map.of(
            "dueDate", "dueDate",
            "scheduledDate", "scheduledDate",
            "priority", "priority",
            "equipmentName", "equipment.name",
            "assetTag", "equipment.assetTag",
            "id", "id"
    );

    private static final Map<String, String> CALIBRATION_HISTORY_SORT_FIELDS = Map.of(
            "calibrationDate", "calibrationDate",
            "nextDueDate", "nextDueDate",
            "calibrationResult", "result",
            "equipmentName", "equipment.name",
            "assetTag", "equipment.assetTag",
            "id", "id"
    );

    private static final Map<String, String> EQUIPMENT_SORT_FIELDS = Map.of(
            "assetTag", "assetTag",
            "equipmentName", "name",
            "serialNumber", "serialNumber",
            "installationDate", "installationDate",
            "status", "status",
            "laboratory", "laboratory.name",
            "id", "id"
    );

    private static final Map<String, String> LABORATORY_SORT_FIELDS = Map.of(
            "laboratoryName", "name",
            "name", "name",
            "labCode", "labCode",
            "department", "department",
            "status", "status",
            "id", "id"
    );

    private static final Map<String, String> STAFF_ASSIGNMENT_SORT_FIELDS = Map.of(
            "staffName", "staff.firstName",
            "roleInLab", "roleInLab",
            "laboratory", "laboratory.name",
            "assignedAt", "assignedAt",
            "id", "id"
    );

    private static final Map<String, String> COMPLIANCE_ATTENTION_SORT_FIELDS = Map.of(
            "category", "category",
            "severity", "severity",
            "equipmentName", "equipmentName",
            "assetTag", "assetTag",
            "relevantDate", "relevantDate"
    );

    @Autowired
    public ReportServiceImpl(EquipmentRepository equipmentRepository,
                             CalibrationScheduleRepository calibrationScheduleRepository,
                             CalibrationRecordRepository calibrationRecordRepository,
                             CalibrationDocumentRepository calibrationDocumentRepository,
                             LaboratoryRepository laboratoryRepository,
                             LaboratoryStaffRepository laboratoryStaffRepository,
                             DataScopeService dataScopeService,
                             AuditService auditService,
                             CalibrationScheduleService calibrationScheduleService,
                             Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.calibrationScheduleRepository = calibrationScheduleRepository;
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.calibrationDocumentRepository = calibrationDocumentRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.dataScopeService = dataScopeService;
        this.auditService = auditService;
        this.calibrationScheduleService = calibrationScheduleService;
        this.clock = clock;
    }

    private LocalDate getToday() {
        return LocalDate.now(clock);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ApiException("From date (" + fromDate + ") cannot be after To date (" + toDate + ")", HttpStatus.BAD_REQUEST);
        }
    }

    private void validatePagination(int page, int size) {
        com.calibratesys.util.PaginationValidator.validate(page, size);
    }

    private int sanitizePage(int page) {
        return Math.max(0, page);
    }

    private int sanitizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private void auditExport(String reportType, ScopedContext ctx, int recordCount) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("reportType", reportType);
        meta.put("format", "CSV");
        meta.put("laboratoryScope", ctx.isUnrestricted() ? "ALL" : String.valueOf(ctx.laboratoryIds()));
        meta.put("recordCount", recordCount);

        auditService.logEvent(
                AuditAction.REPORT_EXPORTED,
                AuditEntityType.REPORT,
                null,
                "Exported " + reportType + " report to CSV (" + recordCount + " records)",
                null,
                meta
        );
    }

    private String resolveResponsibleStaffNames(Laboratory laboratory) {
        if (laboratory == null) {
            return "Unassigned";
        }
        List<LaboratoryStaff> activeAssignments = laboratoryStaffRepository.findActiveAssignmentsByLaboratoryId(laboratory.getId());
        if (activeAssignments.isEmpty()) {
            return "Unassigned";
        }

        // Case A — Active Primary Contact
        List<Staff> primaryContacts = activeAssignments.stream()
                .filter(LaboratoryStaff::isPrimaryContact)
                .map(LaboratoryStaff::getStaff)
                .distinct()
                .toList();

        if (!primaryContacts.isEmpty()) {
            return primaryContacts.stream()
                    .map(s -> s.getFirstName() + " " + s.getLastName())
                    .collect(Collectors.joining(", "));
        }

        // Case B — Calibration coordinator / lab technician
        List<Staff> specializedStaff = activeAssignments.stream()
                .filter(a -> a.getRoleInLab() == StaffLabRole.CALIBRATION_COORDINATOR ||
                             a.getRoleInLab() == StaffLabRole.LAB_TECHNICIAN)
                .map(LaboratoryStaff::getStaff)
                .distinct()
                .toList();

        if (!specializedStaff.isEmpty()) {
            return specializedStaff.stream()
                    .map(s -> s.getFirstName() + " " + s.getLastName())
                    .collect(Collectors.joining(", "));
        }

        // Case C — Fallback
        return activeAssignments.stream()
                .map(LaboratoryStaff::getStaff)
                .distinct()
                .map(s -> s.getFirstName() + " " + s.getLastName())
                .collect(Collectors.joining(", "));
    }

    // =========================================================================
    // 1. CALIBRATION STATUS REPORT
    // =========================================================================

    @Override
    public PageResponseDto<CalibrationStatusReportDto> getCalibrationStatusReport(
            Long laboratoryId, Long equipmentId, EquipmentStatus equipmentStatus,
            CalibrationResult calibrationResult, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "equipmentName", Sort.Direction.ASC, CALIBRATION_STATUS_SORT_FIELDS
        );

        List<CalibrationStatusReportDto> allItems = buildCalibrationStatusItems(
                ctx, laboratoryId, equipmentId, equipmentStatus, calibrationResult, priority, fromDate, toDate, sort
        );

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        int fromIndex = sanitizedPage * sanitizedSize;
        if (fromIndex >= allItems.size()) {
            return PageResponseDto.of(Collections.emptyList(), sanitizedPage, sanitizedSize, allItems.size());
        }
        int toIndex = Math.min(fromIndex + sanitizedSize, allItems.size());
        List<CalibrationStatusReportDto> paged = allItems.subList(fromIndex, toIndex);

        return PageResponseDto.of(paged, sanitizedPage, sanitizedSize, allItems.size());
    }

    @Override
    @Transactional
    public byte[] exportCalibrationStatusReportCsv(
            Long laboratoryId, Long equipmentId, EquipmentStatus equipmentStatus,
            CalibrationResult calibrationResult, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection) {

        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            auditExport("Calibration Status", ctx, 0);
            return CsvExportUtil.generateCsvBytes(
                    List.of("Equipment ID", "Asset Tag", "Equipment Name", "Laboratory", "Status",
                            "Latest Calibration Date", "Next Due Date", "Latest Result", "Priority"),
                    Collections.emptyList()
            );
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "equipmentName", Sort.Direction.ASC, CALIBRATION_STATUS_SORT_FIELDS
        );

        List<CalibrationStatusReportDto> items = buildCalibrationStatusItems(
                ctx, laboratoryId, equipmentId, equipmentStatus, calibrationResult, priority, fromDate, toDate, sort
        );

        auditExport("Calibration Status", ctx, items.size());

        List<String> headers = List.of(
                "Equipment ID", "Asset Tag", "Equipment Name", "Laboratory", "Status",
                "Latest Calibration Date", "Next Due Date", "Latest Result", "Priority"
        );

        List<List<String>> rows = new ArrayList<>();
        for (CalibrationStatusReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getEquipmentId()),
                    item.getAssetTag() != null ? item.getAssetTag() : "",
                    item.getEquipmentName() != null ? item.getEquipmentName() : "",
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getEquipmentStatus() != null ? item.getEquipmentStatus().name() : "",
                    item.getLatestCalibrationDate() != null ? item.getLatestCalibrationDate().toString() : "",
                    item.getNextDueDate() != null ? item.getNextDueDate().toString() : "",
                    item.getLatestCalibrationResult() != null ? item.getLatestCalibrationResult().name() : "",
                    item.getCalibrationPriority() != null ? item.getCalibrationPriority().name() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private List<CalibrationStatusReportDto> buildCalibrationStatusItems(
            ScopedContext ctx, Long laboratoryId, Long equipmentId, EquipmentStatus equipmentStatus,
            CalibrationResult calibrationResult, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate, Sort sort) {

        List<Equipment> equipmentList;
        if (ctx.isUnrestricted()) {
            if (laboratoryId != null) {
                equipmentList = equipmentRepository.findByLaboratoryId(laboratoryId);
            } else {
                equipmentList = equipmentRepository.findAll();
            }
        } else {
            equipmentList = new ArrayList<>();
            for (Long labId : ctx.laboratoryIds()) {
                if (laboratoryId == null || laboratoryId.equals(labId)) {
                    equipmentList.addAll(equipmentRepository.findByLaboratoryId(labId));
                }
            }
        }

        List<CalibrationStatusReportDto> result = new ArrayList<>();

        for (Equipment eq : equipmentList) {
            if (equipmentId != null && !eq.getId().equals(equipmentId)) {
                continue;
            }
            if (equipmentStatus != null && eq.getStatus() != equipmentStatus) {
                continue;
            }

            // Latest calibration record
            List<CalibrationRecord> records = calibrationRecordRepository.findByEquipmentIdOrderByCalibrationDateDesc(eq.getId());
            CalibrationRecord latestRecord = records.isEmpty() ? null : records.get(0);

            // Latest schedule
            List<CalibrationSchedule> schedules = calibrationScheduleRepository.findByEquipmentIdOrderByDueDateAsc(eq.getId());
            CalibrationSchedule activeSchedule = schedules.stream()
                    .filter(s -> s.getStatus() != ScheduleStatus.COMPLETED && s.getStatus() != ScheduleStatus.CANCELLED)
                    .findFirst()
                    .orElse(schedules.isEmpty() ? null : schedules.get(schedules.size() - 1));

            LocalDate latestCalDate = latestRecord != null ? latestRecord.getCalibrationDate() : null;
            LocalDate nextDue = latestRecord != null ? latestRecord.getNextDueDate() : (activeSchedule != null ? activeSchedule.getDueDate() : null);
            CalibrationResult calResult = latestRecord != null ? latestRecord.getResult() : null;
            SchedulePriority calPriority = activeSchedule != null ? activeSchedule.getPriority() : null;

            if (calibrationResult != null && calResult != calibrationResult) {
                continue;
            }
            if (priority != null && calPriority != priority) {
                continue;
            }
            if (fromDate != null) {
                LocalDate refDate = nextDue != null ? nextDue : latestCalDate;
                if (refDate == null || refDate.isBefore(fromDate)) {
                    continue;
                }
            }
            if (toDate != null) {
                LocalDate refDate = nextDue != null ? nextDue : latestCalDate;
                if (refDate == null || refDate.isAfter(toDate)) {
                    continue;
                }
            }

            result.add(new CalibrationStatusReportDto(
                    eq.getId(),
                    eq.getAssetTag(),
                    eq.getName(),
                    eq.getLaboratory() != null ? eq.getLaboratory().getId() : null,
                    eq.getLaboratory() != null ? eq.getLaboratory().getName() : null,
                    eq.getStatus(),
                    latestCalDate,
                    nextDue,
                    calResult,
                    calPriority
            ));
        }

        // Apply sorting
        Comparator<CalibrationStatusReportDto> comparator = Comparator.comparing(
                CalibrationStatusReportDto::getEquipmentName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        for (Sort.Order order : sort) {
            Comparator<CalibrationStatusReportDto> current;
            switch (order.getProperty()) {
                case "name":
                case "equipmentName":
                    current = Comparator.comparing(CalibrationStatusReportDto::getEquipmentName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "assetTag":
                    current = Comparator.comparing(CalibrationStatusReportDto::getAssetTag, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "laboratory.name":
                    current = Comparator.comparing(CalibrationStatusReportDto::getLaboratoryName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "status":
                    current = Comparator.comparing(dto -> dto.getEquipmentStatus() != null ? dto.getEquipmentStatus().name() : "", Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "id":
                default:
                    current = Comparator.comparing(CalibrationStatusReportDto::getEquipmentId, Comparator.nullsLast(Long::compareTo));
                    break;
            }
            if (order.isDescending()) {
                current = current.reversed();
            }
            comparator = current;
        }
        result.sort(comparator);

        return result;
    }

    // =========================================================================
    // 2. UPCOMING CALIBRATIONS REPORT
    // =========================================================================

    @Override
    public PageResponseDto<UpcomingCalibrationReportDto> getUpcomingCalibrationsReport(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        calibrationScheduleService.refreshActiveScheduleStatuses();
        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "dueDate", Sort.Direction.ASC, SCHEDULE_SORT_FIELDS
        );

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        Page<CalibrationSchedule> schedulePage;
        if (ctx.isUnrestricted()) {
            schedulePage = calibrationScheduleRepository.findWithAdvancedFilters(
                    equipmentId, laboratoryId, null, ScheduleStatus.UPCOMING, priority,
                    null, null, fromDate, toDate, null, pageable
            );
        } else {
            schedulePage = calibrationScheduleRepository.findWithAdvancedFiltersAndLaboratoryIds(
                    ctx.laboratoryIds(), equipmentId, laboratoryId, null, ScheduleStatus.UPCOMING, priority,
                    null, null, fromDate, toDate, null, pageable
            );
        }

        List<UpcomingCalibrationReportDto> dtos = schedulePage.getContent().stream()
                .map(this::mapToUpcomingDto)
                .collect(Collectors.toList());

        return PageResponseDto.of(dtos, sanitizedPage, sanitizedSize, schedulePage.getTotalElements());
    }

    @Override
    @Transactional
    public byte[] exportUpcomingCalibrationsReportCsv(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection) {

        calibrationScheduleService.refreshActiveScheduleStatuses();
        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Schedule ID", "Equipment ID", "Asset Tag", "Equipment Name", "Laboratory",
                "Scheduled Date", "Due Date", "Priority", "Status", "Responsible Staff"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Upcoming Calibrations", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "dueDate", Sort.Direction.ASC, SCHEDULE_SORT_FIELDS
        );

        Pageable pageable = PageRequest.of(0, 5000, sort);
        Page<CalibrationSchedule> schedulePage = ctx.isUnrestricted()
                ? calibrationScheduleRepository.findWithAdvancedFilters(
                        equipmentId, laboratoryId, null, ScheduleStatus.UPCOMING, priority,
                        null, null, fromDate, toDate, null, pageable)
                : calibrationScheduleRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), equipmentId, laboratoryId, null, ScheduleStatus.UPCOMING, priority,
                        null, null, fromDate, toDate, null, pageable);

        List<UpcomingCalibrationReportDto> items = schedulePage.getContent().stream()
                .map(this::mapToUpcomingDto)
                .toList();

        auditExport("Upcoming Calibrations", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (UpcomingCalibrationReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getScheduleId()),
                    String.valueOf(item.getEquipmentId()),
                    item.getAssetTag() != null ? item.getAssetTag() : "",
                    item.getEquipmentName() != null ? item.getEquipmentName() : "",
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getScheduledDate() != null ? item.getScheduledDate().toString() : "",
                    item.getDueDate() != null ? item.getDueDate().toString() : "",
                    item.getPriority() != null ? item.getPriority().name() : "",
                    item.getScheduleStatus() != null ? item.getScheduleStatus().name() : "",
                    item.getResponsibleStaff() != null ? item.getResponsibleStaff() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private UpcomingCalibrationReportDto mapToUpcomingDto(CalibrationSchedule s) {
        Laboratory lab = s.getEquipment() != null ? s.getEquipment().getLaboratory() : null;
        return new UpcomingCalibrationReportDto(
                s.getId(),
                s.getEquipment() != null ? s.getEquipment().getId() : null,
                s.getEquipment() != null ? s.getEquipment().getAssetTag() : null,
                s.getEquipment() != null ? s.getEquipment().getName() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getName() : null,
                s.getScheduledDate(),
                s.getDueDate(),
                s.getPriority(),
                s.getStatus(),
                resolveResponsibleStaffNames(lab)
        );
    }

    // =========================================================================
    // 3. DUE CALIBRATIONS REPORT
    // =========================================================================

    @Override
    public PageResponseDto<DueCalibrationReportDto> getDueCalibrationsReport(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        calibrationScheduleService.refreshActiveScheduleStatuses();
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "dueDate", Sort.Direction.ASC, SCHEDULE_SORT_FIELDS
        );

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        Page<CalibrationSchedule> schedulePage = ctx.isUnrestricted()
                ? calibrationScheduleRepository.findWithAdvancedFilters(
                        equipmentId, laboratoryId, null, ScheduleStatus.DUE, priority,
                        null, null, null, null, null, pageable)
                : calibrationScheduleRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), equipmentId, laboratoryId, null, ScheduleStatus.DUE, priority,
                        null, null, null, null, null, pageable);

        List<DueCalibrationReportDto> dtos = schedulePage.getContent().stream()
                .map(this::mapToDueDto)
                .collect(Collectors.toList());

        return PageResponseDto.of(dtos, sanitizedPage, sanitizedSize, schedulePage.getTotalElements());
    }

    @Override
    @Transactional
    public byte[] exportDueCalibrationsReportCsv(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            String sortBy, String sortDirection) {

        calibrationScheduleService.refreshActiveScheduleStatuses();
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Schedule ID", "Equipment ID", "Asset Tag", "Equipment Name", "Laboratory",
                "Scheduled Date", "Due Date", "Priority", "Status", "Responsible Staff"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Due Calibrations", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "dueDate", Sort.Direction.ASC, SCHEDULE_SORT_FIELDS
        );

        Pageable pageable = PageRequest.of(0, 5000, sort);
        Page<CalibrationSchedule> schedulePage = ctx.isUnrestricted()
                ? calibrationScheduleRepository.findWithAdvancedFilters(
                        equipmentId, laboratoryId, null, ScheduleStatus.DUE, priority,
                        null, null, null, null, null, pageable)
                : calibrationScheduleRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), equipmentId, laboratoryId, null, ScheduleStatus.DUE, priority,
                        null, null, null, null, null, pageable);

        List<DueCalibrationReportDto> items = schedulePage.getContent().stream()
                .map(this::mapToDueDto)
                .toList();

        auditExport("Due Calibrations", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (DueCalibrationReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getScheduleId()),
                    String.valueOf(item.getEquipmentId()),
                    item.getAssetTag() != null ? item.getAssetTag() : "",
                    item.getEquipmentName() != null ? item.getEquipmentName() : "",
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getScheduledDate() != null ? item.getScheduledDate().toString() : "",
                    item.getDueDate() != null ? item.getDueDate().toString() : "",
                    item.getPriority() != null ? item.getPriority().name() : "",
                    item.getScheduleStatus() != null ? item.getScheduleStatus().name() : "",
                    item.getResponsibleStaff() != null ? item.getResponsibleStaff() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private DueCalibrationReportDto mapToDueDto(CalibrationSchedule s) {
        Laboratory lab = s.getEquipment() != null ? s.getEquipment().getLaboratory() : null;
        return new DueCalibrationReportDto(
                s.getId(),
                s.getEquipment() != null ? s.getEquipment().getId() : null,
                s.getEquipment() != null ? s.getEquipment().getAssetTag() : null,
                s.getEquipment() != null ? s.getEquipment().getName() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getName() : null,
                s.getScheduledDate(),
                s.getDueDate(),
                s.getPriority(),
                s.getStatus(),
                resolveResponsibleStaffNames(lab)
        );
    }

    // =========================================================================
    // 4. OVERDUE CALIBRATIONS REPORT
    // =========================================================================

    @Override
    public PageResponseDto<OverdueCalibrationReportDto> getOverdueCalibrationsReport(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        calibrationScheduleService.refreshActiveScheduleStatuses();
        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "dueDate", Sort.Direction.ASC, SCHEDULE_SORT_FIELDS
        );

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        Page<CalibrationSchedule> schedulePage = ctx.isUnrestricted()
                ? calibrationScheduleRepository.findWithAdvancedFilters(
                        equipmentId, laboratoryId, null, ScheduleStatus.OVERDUE, priority,
                        null, null, fromDate, toDate, null, pageable)
                : calibrationScheduleRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), equipmentId, laboratoryId, null, ScheduleStatus.OVERDUE, priority,
                        null, null, fromDate, toDate, null, pageable);

        List<OverdueCalibrationReportDto> dtos = schedulePage.getContent().stream()
                .map(this::mapToOverdueDto)
                .collect(Collectors.toList());

        return PageResponseDto.of(dtos, sanitizedPage, sanitizedSize, schedulePage.getTotalElements());
    }

    @Override
    @Transactional
    public byte[] exportOverdueCalibrationsReportCsv(
            Long laboratoryId, Long equipmentId, SchedulePriority priority,
            LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection) {

        calibrationScheduleService.refreshActiveScheduleStatuses();
        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Schedule ID", "Equipment ID", "Asset Tag", "Equipment Name", "Laboratory",
                "Scheduled Date", "Due Date", "Days Overdue", "Priority", "Status", "Responsible Staff"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Overdue Calibrations", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "dueDate", Sort.Direction.ASC, SCHEDULE_SORT_FIELDS
        );

        Pageable pageable = PageRequest.of(0, 5000, sort);
        Page<CalibrationSchedule> schedulePage = ctx.isUnrestricted()
                ? calibrationScheduleRepository.findWithAdvancedFilters(
                        equipmentId, laboratoryId, null, ScheduleStatus.OVERDUE, priority,
                        null, null, fromDate, toDate, null, pageable)
                : calibrationScheduleRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), equipmentId, laboratoryId, null, ScheduleStatus.OVERDUE, priority,
                        null, null, fromDate, toDate, null, pageable);

        List<OverdueCalibrationReportDto> items = schedulePage.getContent().stream()
                .map(this::mapToOverdueDto)
                .toList();

        auditExport("Overdue Calibrations", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (OverdueCalibrationReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getScheduleId()),
                    String.valueOf(item.getEquipmentId()),
                    item.getAssetTag() != null ? item.getAssetTag() : "",
                    item.getEquipmentName() != null ? item.getEquipmentName() : "",
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getScheduledDate() != null ? item.getScheduledDate().toString() : "",
                    item.getDueDate() != null ? item.getDueDate().toString() : "",
                    String.valueOf(item.getDaysOverdue()),
                    item.getPriority() != null ? item.getPriority().name() : "",
                    item.getScheduleStatus() != null ? item.getScheduleStatus().name() : "",
                    item.getResponsibleStaff() != null ? item.getResponsibleStaff() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private OverdueCalibrationReportDto mapToOverdueDto(CalibrationSchedule s) {
        Laboratory lab = s.getEquipment() != null ? s.getEquipment().getLaboratory() : null;
        LocalDate today = getToday();
        long daysOverdue = s.getDueDate() != null && s.getDueDate().isBefore(today)
                ? ChronoUnit.DAYS.between(s.getDueDate(), today)
                : 0L;

        return new OverdueCalibrationReportDto(
                s.getId(),
                s.getEquipment() != null ? s.getEquipment().getId() : null,
                s.getEquipment() != null ? s.getEquipment().getAssetTag() : null,
                s.getEquipment() != null ? s.getEquipment().getName() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getName() : null,
                s.getScheduledDate(),
                s.getDueDate(),
                s.getPriority(),
                s.getStatus(),
                daysOverdue,
                resolveResponsibleStaffNames(lab)
        );
    }

    // =========================================================================
    // 5. CALIBRATION HISTORY REPORT
    // =========================================================================

    @Override
    public PageResponseDto<CalibrationHistoryReportDto> getCalibrationHistoryReport(
            Long laboratoryId, Long equipmentId, CalibrationResult calibrationResult,
            CalibratedByType calibratorType, LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "calibrationDate", Sort.Direction.DESC, CALIBRATION_HISTORY_SORT_FIELDS
        );

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        Page<CalibrationRecord> recordPage = ctx.isUnrestricted()
                ? calibrationRecordRepository.findWithAdvancedFilters(
                        equipmentId, laboratoryId, calibrationResult, calibratorType, null,
                        fromDate, toDate, null, null, null, pageable)
                : calibrationRecordRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), equipmentId, laboratoryId, calibrationResult, calibratorType, null,
                        fromDate, toDate, null, null, null, pageable);

        List<CalibrationHistoryReportDto> dtos = recordPage.getContent().stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());

        return PageResponseDto.of(dtos, sanitizedPage, sanitizedSize, recordPage.getTotalElements());
    }

    @Override
    @Transactional
    public byte[] exportCalibrationHistoryReportCsv(
            Long laboratoryId, Long equipmentId, CalibrationResult calibrationResult,
            CalibratedByType calibratorType, LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection) {

        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Record ID", "Equipment ID", "Asset Tag", "Equipment Name", "Laboratory",
                "Calibration Date", "Next Due Date", "Result", "Calibrator Type",
                "Calibrator Name", "Standards Used", "Uncertainty", "Notes"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Calibration History", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "calibrationDate", Sort.Direction.DESC, CALIBRATION_HISTORY_SORT_FIELDS
        );

        Pageable pageable = PageRequest.of(0, 5000, sort);
        Page<CalibrationRecord> recordPage = ctx.isUnrestricted()
                ? calibrationRecordRepository.findWithAdvancedFilters(
                        equipmentId, laboratoryId, calibrationResult, calibratorType, null,
                        fromDate, toDate, null, null, null, pageable)
                : calibrationRecordRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), equipmentId, laboratoryId, calibrationResult, calibratorType, null,
                        fromDate, toDate, null, null, null, pageable);

        List<CalibrationHistoryReportDto> items = recordPage.getContent().stream()
                .map(this::mapToHistoryDto)
                .toList();

        auditExport("Calibration History", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (CalibrationHistoryReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getRecordId()),
                    String.valueOf(item.getEquipmentId()),
                    item.getAssetTag() != null ? item.getAssetTag() : "",
                    item.getEquipmentName() != null ? item.getEquipmentName() : "",
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getCalibrationDate() != null ? item.getCalibrationDate().toString() : "",
                    item.getNextDueDate() != null ? item.getNextDueDate().toString() : "",
                    item.getCalibrationResult() != null ? item.getCalibrationResult().name() : "",
                    item.getCalibratorType() != null ? item.getCalibratorType().name() : "",
                    item.getCalibratorName() != null ? item.getCalibratorName() : "",
                    item.getStandardsUsed() != null ? item.getStandardsUsed() : "",
                    item.getUncertaintyMeasurement() != null ? item.getUncertaintyMeasurement() : "",
                    item.getNotes() != null ? item.getNotes() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private CalibrationHistoryReportDto mapToHistoryDto(CalibrationRecord r) {
        Laboratory lab = r.getEquipment() != null ? r.getEquipment().getLaboratory() : null;
        return new CalibrationHistoryReportDto(
                r.getId(),
                r.getEquipment() != null ? r.getEquipment().getId() : null,
                r.getEquipment() != null ? r.getEquipment().getAssetTag() : null,
                r.getEquipment() != null ? r.getEquipment().getName() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getName() : null,
                r.getCalibrationDate(),
                r.getNextDueDate(),
                r.getResult(),
                r.getCalibratedByType(),
                r.getCalibratorName(),
                r.getStandardsUsed(),
                r.getUncertaintyMeasurement(),
                r.getNotes()
        );
    }

    // =========================================================================
    // 6. EQUIPMENT INVENTORY REPORT
    // =========================================================================

    @Override
    public PageResponseDto<EquipmentReportDto> getEquipmentReport(
            Long laboratoryId, EquipmentStatus equipmentStatus, String category,
            String manufacturer, LocalDate fromDate, LocalDate toDate,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "equipmentName", Sort.Direction.ASC, EQUIPMENT_SORT_FIELDS
        );

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        Page<Equipment> equipmentPage = ctx.isUnrestricted()
                ? equipmentRepository.findWithAdvancedFilters(
                        laboratoryId, equipmentStatus, manufacturer, null, null, null, null,
                        fromDate, toDate, category, pageable)
                : equipmentRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), laboratoryId, equipmentStatus, manufacturer, null, null, null, null,
                        fromDate, toDate, category, pageable);

        List<EquipmentReportDto> dtos = equipmentPage.getContent().stream()
                .map(this::mapToEquipmentReportDto)
                .collect(Collectors.toList());

        return PageResponseDto.of(dtos, sanitizedPage, sanitizedSize, equipmentPage.getTotalElements());
    }

    @Override
    @Transactional
    public byte[] exportEquipmentReportCsv(
            Long laboratoryId, EquipmentStatus equipmentStatus, String category,
            String manufacturer, LocalDate fromDate, LocalDate toDate,
            String sortBy, String sortDirection) {

        validateDateRange(fromDate, toDate);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Equipment ID", "Asset Tag", "Serial Number", "Equipment Name", "Model",
                "Manufacturer", "Laboratory", "Installation Date", "Calibration Frequency (Days)",
                "Status", "Latest Calibration Date", "Next Due Date"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Equipment Inventory", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "equipmentName", Sort.Direction.ASC, EQUIPMENT_SORT_FIELDS
        );

        Pageable pageable = PageRequest.of(0, 5000, sort);
        Page<Equipment> equipmentPage = ctx.isUnrestricted()
                ? equipmentRepository.findWithAdvancedFilters(
                        laboratoryId, equipmentStatus, manufacturer, null, null, null, null,
                        fromDate, toDate, category, pageable)
                : equipmentRepository.findWithAdvancedFiltersAndLaboratoryIds(
                        ctx.laboratoryIds(), laboratoryId, equipmentStatus, manufacturer, null, null, null, null,
                        fromDate, toDate, category, pageable);

        List<EquipmentReportDto> items = equipmentPage.getContent().stream()
                .map(this::mapToEquipmentReportDto)
                .toList();

        auditExport("Equipment Inventory", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (EquipmentReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getEquipmentId()),
                    item.getAssetTag() != null ? item.getAssetTag() : "",
                    item.getSerialNumber() != null ? item.getSerialNumber() : "",
                    item.getEquipmentName() != null ? item.getEquipmentName() : "",
                    item.getModelNumber() != null ? item.getModelNumber() : "",
                    item.getManufacturer() != null ? item.getManufacturer() : "",
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getInstallationDate() != null ? item.getInstallationDate().toString() : "",
                    item.getCalibrationFrequencyDays() != null ? item.getCalibrationFrequencyDays().toString() : "",
                    item.getCurrentStatus() != null ? item.getCurrentStatus().name() : "",
                    item.getLatestCalibrationDate() != null ? item.getLatestCalibrationDate().toString() : "",
                    item.getNextDueDate() != null ? item.getNextDueDate().toString() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private EquipmentReportDto mapToEquipmentReportDto(Equipment eq) {
        List<CalibrationRecord> records = calibrationRecordRepository.findByEquipmentIdOrderByCalibrationDateDesc(eq.getId());
        CalibrationRecord latestRecord = records.isEmpty() ? null : records.get(0);

        List<CalibrationSchedule> schedules = calibrationScheduleRepository.findByEquipmentIdOrderByDueDateAsc(eq.getId());
        CalibrationSchedule activeSchedule = schedules.stream()
                .filter(s -> s.getStatus() != ScheduleStatus.COMPLETED && s.getStatus() != ScheduleStatus.CANCELLED)
                .findFirst()
                .orElse(schedules.isEmpty() ? null : schedules.get(schedules.size() - 1));

        LocalDate latestCalDate = latestRecord != null ? latestRecord.getCalibrationDate() : null;
        LocalDate nextDue = latestRecord != null ? latestRecord.getNextDueDate() : (activeSchedule != null ? activeSchedule.getDueDate() : null);

        return new EquipmentReportDto(
                eq.getId(),
                eq.getAssetTag(),
                eq.getSerialNumber(),
                eq.getName(),
                eq.getModelNumber(),
                eq.getManufacturer(),
                eq.getLaboratory() != null ? eq.getLaboratory().getId() : null,
                eq.getLaboratory() != null ? eq.getLaboratory().getName() : null,
                eq.getInstallationDate(),
                eq.getCalibrationFrequencyDays(),
                eq.getStatus(),
                latestCalDate,
                nextDue
        );
    }

    // =========================================================================
    // 7. LABORATORY REPORT
    // =========================================================================

    @Override
    public PageResponseDto<LaboratoryReportDto> getLaboratoryReport(
            Long laboratoryId, LabStatus status, String department,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "laboratoryName", Sort.Direction.ASC, LABORATORY_SORT_FIELDS
        );

        List<LaboratoryReportDto> allItems = buildLaboratoryReportItems(ctx, laboratoryId, status, department, sort);

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        int fromIndex = sanitizedPage * sanitizedSize;
        if (fromIndex >= allItems.size()) {
            return PageResponseDto.of(Collections.emptyList(), sanitizedPage, sanitizedSize, allItems.size());
        }
        int toIndex = Math.min(fromIndex + sanitizedSize, allItems.size());
        List<LaboratoryReportDto> paged = allItems.subList(fromIndex, toIndex);

        return PageResponseDto.of(paged, sanitizedPage, sanitizedSize, allItems.size());
    }

    @Override
    @Transactional
    public byte[] exportLaboratoryReportCsv(
            Long laboratoryId, LabStatus status, String department,
            String sortBy, String sortDirection) {

        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Laboratory ID", "Laboratory Name", "Lab Code", "College", "Department",
                "Location", "Status", "Total Equipment", "Active Staff", "Attention Required"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Laboratory Summary", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "laboratoryName", Sort.Direction.ASC, LABORATORY_SORT_FIELDS
        );

        List<LaboratoryReportDto> items = buildLaboratoryReportItems(ctx, laboratoryId, status, department, sort);
        auditExport("Laboratory Summary", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (LaboratoryReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getLaboratoryId()),
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getLaboratoryCode() != null ? item.getLaboratoryCode() : "",
                    item.getCollegeName() != null ? item.getCollegeName() : "",
                    item.getDepartment() != null ? item.getDepartment() : "",
                    item.getLocation() != null ? item.getLocation() : "",
                    item.getLaboratoryStatus() != null ? item.getLaboratoryStatus().name() : "",
                    String.valueOf(item.getTotalEquipmentCount()),
                    String.valueOf(item.getActiveStaffCount()),
                    String.valueOf(item.getCalibrationAttentionCount())
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private List<LaboratoryReportDto> buildLaboratoryReportItems(
            ScopedContext ctx, Long laboratoryId, LabStatus status, String department, Sort sort) {

        calibrationScheduleService.refreshActiveScheduleStatuses();

        List<Laboratory> labs;
        if (ctx.isUnrestricted()) {
            if (laboratoryId != null) {
                labs = laboratoryRepository.findById(laboratoryId).map(List::of).orElse(Collections.emptyList());
            } else {
                labs = laboratoryRepository.findAll();
            }
        } else {
            labs = new ArrayList<>();
            for (Long id : ctx.laboratoryIds()) {
                if (laboratoryId == null || laboratoryId.equals(id)) {
                    laboratoryRepository.findById(id).ifPresent(labs::add);
                }
            }
        }

        // Equipment counts per lab
        List<Object[]> eqRows = ctx.isUnrestricted()
                ? equipmentRepository.countEquipmentGroupedByLaboratory()
                : equipmentRepository.countEquipmentGroupedByLaboratoryForLaboratoryIds(ctx.laboratoryIds());
        Map<Long, Long> eqMap = new HashMap<>();
        for (Object[] row : eqRows) {
            eqMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        // Active staff per lab
        List<Object[]> staffRows = ctx.isUnrestricted()
                ? laboratoryStaffRepository.countActiveStaffPerLaboratory()
                : laboratoryStaffRepository.countActiveStaffPerLaboratoryForLaboratoryIds(ctx.laboratoryIds());
        Map<Long, Long> staffMap = new HashMap<>();
        for (Object[] row : staffRows) {
            staffMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        // Overdue and due schedules per lab
        List<Object[]> schedRows = ctx.isUnrestricted()
                ? calibrationScheduleRepository.countSchedulesPerLaboratoryAndStatus()
                : calibrationScheduleRepository.countSchedulesPerLaboratoryAndStatusForLaboratoryIds(ctx.laboratoryIds());
        Map<String, Long> schedMap = new HashMap<>();
        for (Object[] row : schedRows) {
            Long lId = ((Number) row[0]).longValue();
            ScheduleStatus schedStatus = (ScheduleStatus) row[1];
            Long count = ((Number) row[2]).longValue();
            schedMap.put(lId + "_" + schedStatus.name(), count);
        }

        // Out of calibration equipment per lab
        List<Object[]> outOfCalRows = ctx.isUnrestricted()
                ? equipmentRepository.countEquipmentByStatusGroupedByLaboratory(EquipmentStatus.OUT_OF_CALIBRATION)
                : equipmentRepository.countEquipmentByStatusGroupedByLaboratoryForLaboratoryIds(ctx.laboratoryIds(), EquipmentStatus.OUT_OF_CALIBRATION);
        Map<Long, Long> outOfCalMap = new HashMap<>();
        for (Object[] row : outOfCalRows) {
            outOfCalMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        List<LaboratoryReportDto> result = new ArrayList<>();
        for (Laboratory lab : labs) {
            if (status != null && lab.getStatus() != status) {
                continue;
            }
            if (department != null && !department.trim().isEmpty() &&
                (lab.getDepartment() == null || !lab.getDepartment().equalsIgnoreCase(department.trim()))) {
                continue;
            }

            long eqCount = eqMap.getOrDefault(lab.getId(), 0L);
            long activeStaff = staffMap.getOrDefault(lab.getId(), 0L);
            long overdueCount = schedMap.getOrDefault(lab.getId() + "_" + ScheduleStatus.OVERDUE.name(), 0L);
            long dueCount = schedMap.getOrDefault(lab.getId() + "_" + ScheduleStatus.DUE.name(), 0L);
            long outOfCalCount = outOfCalMap.getOrDefault(lab.getId(), 0L);
            long attentionCount = overdueCount + dueCount + outOfCalCount;

            result.add(new LaboratoryReportDto(
                    lab.getId(),
                    lab.getName(),
                    lab.getLabCode(),
                    lab.getCollege() != null ? lab.getCollege().getId() : null,
                    lab.getCollege() != null ? lab.getCollege().getName() : null,
                    lab.getDepartment(),
                    lab.getLocation(),
                    lab.getStatus(),
                    eqCount,
                    activeStaff,
                    attentionCount
            ));
        }

        Comparator<LaboratoryReportDto> comparator = Comparator.comparing(
                LaboratoryReportDto::getLaboratoryName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        for (Sort.Order order : sort) {
            Comparator<LaboratoryReportDto> current;
            switch (order.getProperty()) {
                case "labCode":
                    current = Comparator.comparing(LaboratoryReportDto::getLaboratoryCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "department":
                    current = Comparator.comparing(LaboratoryReportDto::getDepartment, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "status":
                    current = Comparator.comparing(dto -> dto.getLaboratoryStatus() != null ? dto.getLaboratoryStatus().name() : "", Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "id":
                    current = Comparator.comparing(LaboratoryReportDto::getLaboratoryId, Comparator.nullsLast(Long::compareTo));
                    break;
                case "name":
                case "laboratoryName":
                default:
                    current = Comparator.comparing(LaboratoryReportDto::getLaboratoryName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
            }
            if (order.isDescending()) {
                current = current.reversed();
            }
            comparator = current;
        }
        result.sort(comparator);

        return result;
    }

    // =========================================================================
    // 8. STAFF ASSIGNMENT REPORT
    // =========================================================================

    @Override
    public PageResponseDto<StaffAssignmentReportDto> getStaffAssignmentReport(
            Long laboratoryId, Long staffId, StaffLabRole roleInLab,
            Boolean isPrimaryContact, String status,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "staffName", Sort.Direction.ASC, STAFF_ASSIGNMENT_SORT_FIELDS
        );

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, sort);

        Page<LaboratoryStaff> assignmentPage = ctx.isUnrestricted()
                ? laboratoryStaffRepository.findAssignmentsWithFilters(
                        laboratoryId, staffId, roleInLab, isPrimaryContact, status, pageable)
                : laboratoryStaffRepository.findAssignmentsWithFiltersForLaboratoryIds(
                        ctx.laboratoryIds(), laboratoryId, staffId, roleInLab, isPrimaryContact, status, pageable);

        List<StaffAssignmentReportDto> dtos = assignmentPage.getContent().stream()
                .map(this::mapToAssignmentDto)
                .collect(Collectors.toList());

        return PageResponseDto.of(dtos, sanitizedPage, sanitizedSize, assignmentPage.getTotalElements());
    }

    @Override
    @Transactional
    public byte[] exportStaffAssignmentReportCsv(
            Long laboratoryId, Long staffId, StaffLabRole roleInLab,
            Boolean isPrimaryContact, String status,
            String sortBy, String sortDirection) {

        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Assignment ID", "Staff ID", "Staff Name", "Email", "Designation",
                "Laboratory ID", "Laboratory Name", "Lab Code", "Role In Lab",
                "Primary Contact", "Assignment Status"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Staff Assignments", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "staffName", Sort.Direction.ASC, STAFF_ASSIGNMENT_SORT_FIELDS
        );

        Pageable pageable = PageRequest.of(0, 5000, sort);
        Page<LaboratoryStaff> assignmentPage = ctx.isUnrestricted()
                ? laboratoryStaffRepository.findAssignmentsWithFilters(
                        laboratoryId, staffId, roleInLab, isPrimaryContact, status, pageable)
                : laboratoryStaffRepository.findAssignmentsWithFiltersForLaboratoryIds(
                        ctx.laboratoryIds(), laboratoryId, staffId, roleInLab, isPrimaryContact, status, pageable);

        List<StaffAssignmentReportDto> items = assignmentPage.getContent().stream()
                .map(this::mapToAssignmentDto)
                .toList();

        auditExport("Staff Assignments", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (StaffAssignmentReportDto item : items) {
            rows.add(List.of(
                    String.valueOf(item.getAssignmentId()),
                    item.getStaffId() != null ? item.getStaffId() : "",
                    item.getStaffName() != null ? item.getStaffName() : "",
                    item.getEmail() != null ? item.getEmail() : "",
                    item.getDesignation() != null ? item.getDesignation() : "",
                    String.valueOf(item.getLaboratoryId()),
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getLaboratoryCode() != null ? item.getLaboratoryCode() : "",
                    item.getRoleInLab() != null ? item.getRoleInLab().name() : "",
                    item.isPrimaryContact() ? "YES" : "NO",
                    item.getAssignmentStatus() != null ? item.getAssignmentStatus() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private StaffAssignmentReportDto mapToAssignmentDto(LaboratoryStaff ls) {
        Staff staff = ls.getStaff();
        Laboratory lab = ls.getLaboratory();
        return new StaffAssignmentReportDto(
                ls.getId(),
                staff != null ? staff.getStaffId() : null,
                staff != null ? staff.getId() : null,
                staff != null ? (staff.getFirstName() + " " + staff.getLastName()) : null,
                staff != null ? staff.getEmail() : null,
                staff != null ? staff.getDesignation() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getName() : null,
                lab != null ? lab.getLabCode() : null,
                ls.getRoleInLab(),
                ls.isPrimaryContact(),
                ls.getStatus()
        );
    }

    // =========================================================================
    // 9. COMPLIANCE ATTENTION REPORT
    // =========================================================================

    @Override
    public PageResponseDto<ComplianceAttentionReportDto> getComplianceAttentionReport(
            Long laboratoryId, String category,
            int page, int size, String sortBy, String sortDirection) {

        validatePagination(page, size);
        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        if (ctx.isEmptyScope()) {
            return PageResponseDto.of(Collections.emptyList(), page, size, 0);
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "severity", Sort.Direction.ASC, COMPLIANCE_ATTENTION_SORT_FIELDS
        );

        List<ComplianceAttentionReportDto> allItems = buildComplianceAttentionItems(ctx, laboratoryId, category, sort);

        int sanitizedPage = sanitizePage(page);
        int sanitizedSize = sanitizeSize(size);
        int fromIndex = sanitizedPage * sanitizedSize;
        if (fromIndex >= allItems.size()) {
            return PageResponseDto.of(Collections.emptyList(), sanitizedPage, sanitizedSize, allItems.size());
        }
        int toIndex = Math.min(fromIndex + sanitizedSize, allItems.size());
        List<ComplianceAttentionReportDto> paged = allItems.subList(fromIndex, toIndex);

        return PageResponseDto.of(paged, sanitizedPage, sanitizedSize, allItems.size());
    }

    @Override
    @Transactional
    public byte[] exportComplianceAttentionReportCsv(
            Long laboratoryId, String category,
            String sortBy, String sortDirection) {

        ScopedContext ctx = dataScopeService.resolveScope(laboratoryId);

        List<String> headers = List.of(
                "Category", "Severity", "Equipment ID", "Asset Tag", "Equipment Name",
                "Laboratory", "Details", "Action Required", "Relevant Date"
        );

        if (ctx.isEmptyScope()) {
            auditExport("Compliance Attention", ctx, 0);
            return CsvExportUtil.generateCsvBytes(headers, Collections.emptyList());
        }

        Sort sort = SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "severity", Sort.Direction.ASC, COMPLIANCE_ATTENTION_SORT_FIELDS
        );

        List<ComplianceAttentionReportDto> items = buildComplianceAttentionItems(ctx, laboratoryId, category, sort);
        auditExport("Compliance Attention", ctx, items.size());

        List<List<String>> rows = new ArrayList<>();
        for (ComplianceAttentionReportDto item : items) {
            rows.add(List.of(
                    item.getCategory() != null ? item.getCategory() : "",
                    item.getSeverity() != null ? item.getSeverity() : "",
                    item.getEquipmentId() != null ? String.valueOf(item.getEquipmentId()) : "",
                    item.getAssetTag() != null ? item.getAssetTag() : "",
                    item.getEquipmentName() != null ? item.getEquipmentName() : "",
                    item.getLaboratoryName() != null ? item.getLaboratoryName() : "",
                    item.getDetails() != null ? item.getDetails() : "",
                    item.getActionRequired() != null ? item.getActionRequired() : "",
                    item.getRelevantDate() != null ? item.getRelevantDate().toString() : ""
            ));
        }

        return CsvExportUtil.generateCsvBytes(headers, rows);
    }

    private List<ComplianceAttentionReportDto> buildComplianceAttentionItems(
            ScopedContext ctx, Long laboratoryId, String categoryFilter, Sort sort) {

        calibrationScheduleService.refreshActiveScheduleStatuses();
        LocalDate today = getToday();
        List<ComplianceAttentionReportDto> items = new ArrayList<>();

        boolean allCategories = categoryFilter == null || categoryFilter.trim().isEmpty() || "ALL".equalsIgnoreCase(categoryFilter.trim());
        String cat = categoryFilter != null ? categoryFilter.trim().toUpperCase() : "";

        // 1. OVERDUE_CALIBRATION
        if (allCategories || "OVERDUE_CALIBRATION".equals(cat)) {
            List<CalibrationSchedule> overdue = ctx.isUnrestricted()
                    ? calibrationScheduleRepository.findByStatusWithEquipmentAndLaboratory(ScheduleStatus.OVERDUE)
                    : calibrationScheduleRepository.findByStatusAndLaboratoryIdInWithEquipmentAndLaboratory(ScheduleStatus.OVERDUE, ctx.laboratoryIds());

            for (CalibrationSchedule s : overdue) {
                if (laboratoryId != null && (s.getEquipment() == null || s.getEquipment().getLaboratory() == null || !s.getEquipment().getLaboratory().getId().equals(laboratoryId))) {
                    continue;
                }
                Equipment eq = s.getEquipment();
                long daysOverdue = s.getDueDate() != null ? ChronoUnit.DAYS.between(s.getDueDate(), today) : 0L;
                items.add(new ComplianceAttentionReportDto(
                        "OVERDUE_CALIBRATION",
                        "CRITICAL",
                        eq != null ? eq.getId() : null,
                        eq != null ? eq.getAssetTag() : null,
                        eq != null ? eq.getName() : null,
                        eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getId() : null,
                        eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getName() : null,
                        "Calibration is overdue since " + s.getDueDate() + " (" + daysOverdue + " days overdue)",
                        "Perform urgent calibration",
                        s.getDueDate()
                ));
            }
        }

        // 2. DUE_CALIBRATION
        if (allCategories || "DUE_CALIBRATION".equals(cat)) {
            List<CalibrationSchedule> due = ctx.isUnrestricted()
                    ? calibrationScheduleRepository.findByStatusWithEquipmentAndLaboratory(ScheduleStatus.DUE)
                    : calibrationScheduleRepository.findByStatusAndLaboratoryIdInWithEquipmentAndLaboratory(ScheduleStatus.DUE, ctx.laboratoryIds());

            for (CalibrationSchedule s : due) {
                if (laboratoryId != null && (s.getEquipment() == null || s.getEquipment().getLaboratory() == null || !s.getEquipment().getLaboratory().getId().equals(laboratoryId))) {
                    continue;
                }
                Equipment eq = s.getEquipment();
                items.add(new ComplianceAttentionReportDto(
                        "DUE_CALIBRATION",
                        "HIGH",
                        eq != null ? eq.getId() : null,
                        eq != null ? eq.getAssetTag() : null,
                        eq != null ? eq.getName() : null,
                        eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getId() : null,
                        eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getName() : null,
                        "Calibration is due today (" + s.getDueDate() + ")",
                        "Perform calibration today",
                        s.getDueDate()
                ));
            }
        }

        // 3. FAILED_CALIBRATION
        if (allCategories || "FAILED_CALIBRATION".equals(cat)) {
            List<CalibrationRecord> failedRecords = calibrationRecordRepository.findByResult(CalibrationResult.FAILED);
            for (CalibrationRecord r : failedRecords) {
                Equipment eq = r.getEquipment();
                if (eq == null || eq.getLaboratory() == null) {
                    continue;
                }
                Long labId = eq.getLaboratory().getId();
                if (!ctx.isPermitted(labId)) {
                    continue;
                }
                if (laboratoryId != null && !laboratoryId.equals(labId)) {
                    continue;
                }

                items.add(new ComplianceAttentionReportDto(
                        "FAILED_CALIBRATION",
                        "HIGH",
                        eq.getId(),
                        eq.getAssetTag(),
                        eq.getName(),
                        labId,
                        eq.getLaboratory().getName(),
                        "Last calibration FAILED on " + r.getCalibrationDate() + " (" + (r.getCalibratorName() != null ? r.getCalibratorName() : "Unknown") + ")",
                        "Review failure notes and recalibrate/service",
                        r.getCalibrationDate()
                ));
            }
        }

        // 4. EXPIRED_CERTIFICATE
        if (allCategories || "EXPIRED_CERTIFICATE".equals(cat)) {
            List<CalibrationDocument> allDocs = calibrationDocumentRepository.findAll();
            for (CalibrationDocument d : allDocs) {
                if (d.getValidUntil() != null && d.getValidUntil().isBefore(today)) {
                    Equipment eq = d.getEquipment();
                    if (eq == null || eq.getLaboratory() == null) {
                        continue;
                    }
                    Long labId = eq.getLaboratory().getId();
                    if (!ctx.isPermitted(labId)) {
                        continue;
                    }
                    if (laboratoryId != null && !laboratoryId.equals(labId)) {
                        continue;
                    }

                    String docIdentifier = d.getCertificateNumber() != null ? d.getCertificateNumber() : d.getDocumentName();
                    items.add(new ComplianceAttentionReportDto(
                            "EXPIRED_CERTIFICATE",
                            "MEDIUM",
                            eq.getId(),
                            eq.getAssetTag(),
                            eq.getName(),
                            labId,
                            eq.getLaboratory().getName(),
                            "Certificate " + docIdentifier + " expired on " + d.getValidUntil(),
                            "Renew calibration certificate",
                            d.getValidUntil()
                    ));
                }
            }
        }

        // 5. OUT_OF_CALIBRATION
        if (allCategories || "OUT_OF_CALIBRATION".equals(cat)) {
            List<Equipment> outOfCalEquip = equipmentRepository.findByStatus(EquipmentStatus.OUT_OF_CALIBRATION);
            for (Equipment eq : outOfCalEquip) {
                if (eq.getLaboratory() == null) {
                    continue;
                }
                Long labId = eq.getLaboratory().getId();
                if (!ctx.isPermitted(labId)) {
                    continue;
                }
                if (laboratoryId != null && !laboratoryId.equals(labId)) {
                    continue;
                }

                items.add(new ComplianceAttentionReportDto(
                        "OUT_OF_CALIBRATION",
                        "CRITICAL",
                        eq.getId(),
                        eq.getAssetTag(),
                        eq.getName(),
                        labId,
                        eq.getLaboratory().getName(),
                        "Equipment status is OUT_OF_CALIBRATION",
                        "Remove from active service until calibrated",
                        today
                ));
            }
        }

        // Apply sorting
        Comparator<ComplianceAttentionReportDto> comparator = Comparator.comparing(
                ComplianceAttentionReportDto::getSeverity, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        );
        for (Sort.Order order : sort) {
            Comparator<ComplianceAttentionReportDto> current;
            switch (order.getProperty()) {
                case "category":
                    current = Comparator.comparing(ComplianceAttentionReportDto::getCategory, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "equipmentName":
                    current = Comparator.comparing(ComplianceAttentionReportDto::getEquipmentName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "assetTag":
                    current = Comparator.comparing(ComplianceAttentionReportDto::getAssetTag, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
                case "relevantDate":
                    current = Comparator.comparing(ComplianceAttentionReportDto::getRelevantDate, Comparator.nullsLast(LocalDate::compareTo));
                    break;
                case "severity":
                default:
                    current = Comparator.comparing(ComplianceAttentionReportDto::getSeverity, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                    break;
            }
            if (order.isDescending()) {
                current = current.reversed();
            }
            comparator = current;
        }
        items.sort(comparator);

        return items;
    }
}
