package com.calibratesys.service.impl;

import com.calibratesys.dto.CalibrationRecordRequestDto;
import com.calibratesys.dto.CalibrationRecordResponseDto;
import com.calibratesys.entity.CalibrationRecord;
import com.calibratesys.entity.CalibrationSchedule;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;
import com.calibratesys.entity.enums.EquipmentStatus;
import com.calibratesys.entity.enums.ScheduleStatus;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.CalibrationRecordRepository;
import com.calibratesys.repository.CalibrationScheduleRepository;
import com.calibratesys.repository.EquipmentRepository;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import com.calibratesys.service.AuditService;
import com.calibratesys.service.CalibrationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CalibrationRecordServiceImpl implements CalibrationRecordService {

    private final CalibrationRecordRepository calibrationRecordRepository;
    private final EquipmentRepository equipmentRepository;
    private final CalibrationScheduleRepository scheduleRepository;
    private final AuditService auditService;
    private final com.calibratesys.security.DataScopeService dataScopeService;

    @Autowired
    public CalibrationRecordServiceImpl(CalibrationRecordRepository calibrationRecordRepository,
                                        EquipmentRepository equipmentRepository,
                                        CalibrationScheduleRepository scheduleRepository,
                                        AuditService auditService,
                                        com.calibratesys.security.DataScopeService dataScopeService) {
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.equipmentRepository = equipmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.auditService = auditService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public CalibrationRecordResponseDto createRecord(CalibrationRecordRequestDto requestDto) {
        Equipment equipment = equipmentRepository.findById(requestDto.getEquipmentId())
                .orElseThrow(() -> new ApiException("Equipment not found with ID: " + requestDto.getEquipmentId(), HttpStatus.NOT_FOUND));

        if (requestDto.getNextDueDate().isBefore(requestDto.getCalibrationDate())) {
            throw new ApiException("Next due date cannot be before calibration date", HttpStatus.BAD_REQUEST);
        }

        CalibrationSchedule schedule = null;
        if (requestDto.getScheduleId() != null) {
            schedule = scheduleRepository.findById(requestDto.getScheduleId())
                    .orElseThrow(() -> new ApiException("Calibration schedule not found with ID: " + requestDto.getScheduleId(), HttpStatus.NOT_FOUND));

            if (!schedule.getEquipment().getId().equals(equipment.getId())) {
                throw new ApiException("Schedule #" + requestDto.getScheduleId() + " belongs to equipment #" +
                        schedule.getEquipment().getId() + " (" + schedule.getEquipment().getAssetTag() + "), not equipment #" +
                        equipment.getId() + " (" + equipment.getAssetTag() + ")", HttpStatus.BAD_REQUEST);
            }

            // Mark schedule as COMPLETED
            schedule.setStatus(ScheduleStatus.COMPLETED);
            scheduleRepository.save(schedule);
        }

        // Operational synchronization: if PASSED, return equipment to OPERATIONAL status if out/due
        if (requestDto.getResult() == CalibrationResult.PASSED) {
            if (equipment.getStatus() == EquipmentStatus.CALIBRATION_DUE ||
                equipment.getStatus() == EquipmentStatus.OUT_OF_CALIBRATION) {
                equipment.setStatus(EquipmentStatus.OPERATIONAL);
                equipmentRepository.save(equipment);
            }
        }

        CalibrationRecord record = new CalibrationRecord(
                equipment,
                schedule,
                requestDto.getCalibrationDate(),
                requestDto.getNextDueDate(),
                requestDto.getCalibratedByType(),
                requestDto.getCalibratorName(),
                requestDto.getResult(),
                requestDto.getUncertaintyMeasurement(),
                requestDto.getStandardsUsed(),
                requestDto.getNotes()
        );

        CalibrationRecord saved = calibrationRecordRepository.save(record);

        auditService.logEvent(
                AuditAction.CALIBRATION_RECORD_CREATED,
                AuditEntityType.CALIBRATION_RECORD,
                saved.getId(),
                "Calibration record #" + saved.getId() + " was created for equipment " + equipment.getAssetTag() + " with result " + saved.getResult() + ".",
                null,
                Map.of(
                        "equipmentId", equipment.getId(),
                        "assetTag", equipment.getAssetTag(),
                        "calibrationDate", saved.getCalibrationDate().toString(),
                        "nextDueDate", saved.getNextDueDate().toString(),
                        "result", saved.getResult().name(),
                        "calibratedByType", saved.getCalibratedByType().name(),
                        "calibratorName", saved.getCalibratorName() != null ? saved.getCalibratorName() : ""
                )
        );

        return mapToResponseDto(saved);
    }

    @Override
    public CalibrationRecordResponseDto getRecordById(Long id) {
        CalibrationRecord record = calibrationRecordRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration record not found with ID: " + id, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = record.getEquipment() != null && record.getEquipment().getLaboratory() != null
                ? record.getEquipment().getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access calibration record in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        return mapToResponseDto(record);
    }

    @Override
    public List<CalibrationRecordResponseDto> getRecords(Long equipmentId, Long laboratoryId,
                                                        CalibrationResult result, CalibratedByType calibratedByType,
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

        List<CalibrationRecord> records = calibrationRecordRepository.findWithFilters(
                equipmentId, laboratoryId, result, calibratedByType, fromDate, toDate
        );

        if (scope.laboratoryIds() != null && laboratoryId == null) {
            records = records.stream()
                    .filter(r -> r.getEquipment() != null && r.getEquipment().getLaboratory() != null && scope.isPermitted(r.getEquipment().getLaboratory().getId()))
                    .collect(Collectors.toList());
        }

        return records.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }

    @Override
    public List<CalibrationRecordResponseDto> getRecordsByEquipmentId(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ApiException("Equipment not found with ID: " + equipmentId, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = equipment.getLaboratory() != null ? equipment.getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access calibration records for laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        return calibrationRecordRepository.findByEquipmentIdOrderByCalibrationDateDesc(equipmentId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CalibrationRecordResponseDto> getRecordsByScheduleId(Long scheduleId) {
        CalibrationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ApiException("Calibration schedule not found with ID: " + scheduleId, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = schedule.getEquipment() != null && schedule.getEquipment().getLaboratory() != null
                ? schedule.getEquipment().getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access calibration records for laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        return calibrationRecordRepository.findByCalibrationScheduleIdOrderByCalibrationDateDesc(scheduleId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private static final Map<String, String> RECORD_SORT_FIELDS = Map.of(
            "calibrationDate", "calibrationDate",
            "nextDueDate", "nextDueDate",
            "result", "result",
            "calibratedByType", "calibratedByType",
            "calibratorName", "calibratorName",
            "createdAt", "createdAt"
    );

    @Override
    @Transactional(readOnly = true)
    public com.calibratesys.dto.PageResponseDto<CalibrationRecordResponseDto> searchRecords(
            Long equipmentId,
            Long laboratoryId,
            CalibrationResult result,
            CalibratedByType calibratedByType,
            String calibratorName,
            LocalDate calibrationStartDate,
            LocalDate calibrationEndDate,
            LocalDate dueStartDate,
            LocalDate dueEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        if (calibrationStartDate != null && calibrationEndDate != null && calibrationStartDate.isAfter(calibrationEndDate)) {
            throw new ApiException("Calibration start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }
        if (dueStartDate != null && dueEndDate != null && dueStartDate.isAfter(dueEndDate)) {
            throw new ApiException("Due start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }

        String calName = org.springframework.util.StringUtils.hasText(calibratorName) ? calibratorName.trim() : null;
        String q = org.springframework.util.StringUtils.hasText(search) ? search.trim() : null;

        org.springframework.data.domain.Sort sort = com.calibratesys.util.SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "calibrationDate", org.springframework.data.domain.Sort.Direction.DESC, RECORD_SORT_FIELDS
        );

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(validatedPage, validatedSize, sort);

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope(laboratoryId);
        if (scope.isEmptyScope()) {
            return com.calibratesys.dto.PageResponseDto.from(new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0));
        }

        org.springframework.data.domain.Page<CalibrationRecord> pageResult;
        if (scope.laboratoryIds() != null) {
            pageResult = calibrationRecordRepository.findWithAdvancedFiltersAndLaboratoryIds(
                    scope.laboratoryIds(), equipmentId, laboratoryId, result, calibratedByType,
                    calName, calibrationStartDate, calibrationEndDate, dueStartDate, dueEndDate, q, pageable
            );
        } else {
            pageResult = calibrationRecordRepository.findWithAdvancedFilters(
                    equipmentId, laboratoryId, result, calibratedByType,
                    calName, calibrationStartDate, calibrationEndDate, dueStartDate, dueEndDate, q, pageable
            );
        }

        return com.calibratesys.dto.PageResponseDto.from(pageResult.map(this::mapToResponseDto));
    }

    private CalibrationRecordResponseDto mapToResponseDto(CalibrationRecord r) {
        Equipment e = r.getEquipment();
        Laboratory lab = e != null ? e.getLaboratory() : null;
        College college = lab != null ? lab.getCollege() : null;
        CalibrationSchedule s = r.getCalibrationSchedule();

        return new CalibrationRecordResponseDto(
                r.getId(),
                e != null ? e.getId() : null,
                e != null ? e.getAssetTag() : null,
                e != null ? e.getName() : null,
                e != null ? e.getSerialNumber() : null,
                lab != null ? lab.getId() : null,
                lab != null ? lab.getLabCode() : null,
                lab != null ? lab.getName() : null,
                college != null ? college.getId() : null,
                college != null ? college.getCode() : null,
                college != null ? college.getName() : null,
                s != null ? s.getId() : null,
                r.getCalibrationDate(),
                r.getNextDueDate(),
                r.getCalibratedByType(),
                r.getCalibratorName(),
                r.getResult(),
                r.getUncertaintyMeasurement(),
                r.getStandardsUsed(),
                r.getNotes(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
