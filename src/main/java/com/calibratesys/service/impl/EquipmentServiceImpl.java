package com.calibratesys.service.impl;

import com.calibratesys.dto.EquipmentRequestDto;
import com.calibratesys.dto.EquipmentResponseDto;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.enums.EquipmentStatus;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.CalibrationDocumentRepository;
import com.calibratesys.repository.CalibrationRecordRepository;
import com.calibratesys.repository.CalibrationScheduleRepository;
import com.calibratesys.repository.EquipmentRepository;
import com.calibratesys.repository.LaboratoryRepository;
import com.calibratesys.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;
    private final CalibrationScheduleRepository calibrationScheduleRepository;
    private final CalibrationDocumentRepository calibrationDocumentRepository;
    private final com.calibratesys.service.AuditService auditService;
    private final com.calibratesys.security.DataScopeService dataScopeService;

    @Autowired
    public EquipmentServiceImpl(EquipmentRepository equipmentRepository,
                                LaboratoryRepository laboratoryRepository,
                                CalibrationRecordRepository calibrationRecordRepository,
                                CalibrationScheduleRepository calibrationScheduleRepository,
                                CalibrationDocumentRepository calibrationDocumentRepository,
                                com.calibratesys.service.AuditService auditService,
                                com.calibratesys.security.DataScopeService dataScopeService) {
        this.equipmentRepository = equipmentRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.calibrationScheduleRepository = calibrationScheduleRepository;
        this.calibrationDocumentRepository = calibrationDocumentRepository;
        this.auditService = auditService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    @Transactional
    public EquipmentResponseDto createEquipment(EquipmentRequestDto request) {
        validateRequest(request);

        Laboratory laboratory = laboratoryRepository.findById(request.getLaboratoryId())
                .orElseThrow(() -> new ApiException("Laboratory not found with id: " + request.getLaboratoryId(), HttpStatus.NOT_FOUND));

        String assetTag = request.getAssetTag().trim();
        if (equipmentRepository.existsByAssetTag(assetTag)) {
            throw new ApiException("Equipment with asset tag '" + assetTag + "' already exists", HttpStatus.CONFLICT);
        }

        String serialNumber = StringUtils.hasText(request.getSerialNumber()) ? request.getSerialNumber().trim() : null;
        if (serialNumber != null && equipmentRepository.existsBySerialNumber(serialNumber)) {
            throw new ApiException("Equipment with serial number '" + serialNumber + "' already exists", HttpStatus.CONFLICT);
        }

        EquipmentStatus status = parseStatus(request.getStatus());

        Equipment equipment = new Equipment(
                laboratory,
                assetTag,
                serialNumber,
                request.getName().trim(),
                StringUtils.hasText(request.getModelNumber()) ? request.getModelNumber().trim() : null,
                StringUtils.hasText(request.getManufacturer()) ? request.getManufacturer().trim() : null,
                request.getPurchaseDate(),
                request.getInstallationDate(),
                request.getCalibrationFrequencyDays(),
                status
        );

        Equipment saved = equipmentRepository.save(equipment);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.EQUIPMENT_CREATED,
                com.calibratesys.entity.enums.AuditEntityType.EQUIPMENT,
                saved.getId(),
                "Equipment " + saved.getAssetTag() + " (" + saved.getName() + ") was created.",
                null,
                java.util.Map.of("assetTag", saved.getAssetTag(), "name", saved.getName(), "status", saved.getStatus().name(), "laboratoryId", saved.getLaboratory().getId(), "calibrationFrequencyDays", saved.getCalibrationFrequencyDays())
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentResponseDto getEquipmentById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Equipment not found with id: " + id, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = equipment.getLaboratory() != null ? equipment.getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access equipment in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        return mapToDto(equipment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentResponseDto> getEquipment(Long laboratoryId, String status, String search) {
        List<Equipment> equipmentList;

        boolean hasLab = laboratoryId != null;
        boolean hasStatus = StringUtils.hasText(status);
        boolean hasSearch = StringUtils.hasText(search);

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope(laboratoryId);
        if (scope.isEmptyScope()) {
            return Collections.emptyList();
        }

        if (hasLab && !laboratoryRepository.existsById(laboratoryId)) {
            throw new ApiException("Laboratory not found with id: " + laboratoryId, HttpStatus.NOT_FOUND);
        }

        EquipmentStatus parsedStatus = hasStatus ? parseStatus(status) : null;

        if (hasLab && hasStatus) {
            equipmentList = equipmentRepository.findByLaboratoryIdAndStatus(laboratoryId, parsedStatus);
        } else if (hasLab) {
            equipmentList = equipmentRepository.findByLaboratoryId(laboratoryId);
        } else if (hasStatus) {
            equipmentList = equipmentRepository.findByStatus(parsedStatus);
        } else if (hasSearch) {
            String q = search.trim();
            equipmentList = equipmentRepository
                    .findByNameContainingIgnoreCaseOrAssetTagContainingIgnoreCaseOrSerialNumberContainingIgnoreCase(q, q, q);
        } else {
            equipmentList = equipmentRepository.findAll();
        }

        // Apply laboratory scope filtering for LAB_STAFF if not already filtered by laboratoryId
        if (scope.laboratoryIds() != null && !hasLab) {
            equipmentList = equipmentList.stream()
                    .filter(e -> e.getLaboratory() != null && scope.isPermitted(e.getLaboratory().getId()))
                    .collect(Collectors.toList());
        }

        // Apply secondary search filtering in-memory if laboratory/status was also combined with search
        if (hasSearch && (hasLab || hasStatus)) {
            String q = search.trim().toLowerCase();
            equipmentList = equipmentList.stream()
                    .filter(e -> (e.getName() != null && e.getName().toLowerCase().contains(q)) ||
                            (e.getAssetTag() != null && e.getAssetTag().toLowerCase().contains(q)) ||
                            (e.getSerialNumber() != null && e.getSerialNumber().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        return equipmentList.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private static final java.util.Map<String, String> EQUIPMENT_SORT_FIELDS = java.util.Map.of(
            "name", "name",
            "assetTag", "assetTag",
            "serialNumber", "serialNumber",
            "modelNumber", "modelNumber",
            "manufacturer", "manufacturer",
            "purchaseDate", "purchaseDate",
            "installationDate", "installationDate",
            "calibrationFrequencyDays", "calibrationFrequencyDays",
            "status", "status",
            "createdAt", "createdAt"
    );

    @Override
    @Transactional(readOnly = true)
    public com.calibratesys.dto.PageResponseDto<EquipmentResponseDto> searchEquipment(
            Long laboratoryId,
            String status,
            String manufacturer,
            String modelNumber,
            Integer calibrationFrequencyDays,
            java.time.LocalDate purchaseStartDate,
            java.time.LocalDate purchaseEndDate,
            java.time.LocalDate installationStartDate,
            java.time.LocalDate installationEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        if (purchaseStartDate != null && purchaseEndDate != null && purchaseStartDate.isAfter(purchaseEndDate)) {
            throw new ApiException("Purchase start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }
        if (installationStartDate != null && installationEndDate != null && installationStartDate.isAfter(installationEndDate)) {
            throw new ApiException("Installation start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }

        EquipmentStatus parsedStatus = StringUtils.hasText(status) ? parseStatus(status) : null;
        String mfr = StringUtils.hasText(manufacturer) ? manufacturer.trim() : null;
        String mdl = StringUtils.hasText(modelNumber) ? modelNumber.trim() : null;
        String q = StringUtils.hasText(search) ? search.trim() : null;

        org.springframework.data.domain.Sort sort = com.calibratesys.util.SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "name", org.springframework.data.domain.Sort.Direction.ASC, EQUIPMENT_SORT_FIELDS
        );

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(validatedPage, validatedSize, sort);

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope(laboratoryId);
        if (scope.isEmptyScope()) {
            return com.calibratesys.dto.PageResponseDto.from(new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0));
        }

        org.springframework.data.domain.Page<Equipment> pageResult;
        if (scope.laboratoryIds() != null) {
            pageResult = equipmentRepository.findWithAdvancedFiltersAndLaboratoryIds(
                    scope.laboratoryIds(), laboratoryId, parsedStatus, mfr, mdl,
                    calibrationFrequencyDays, purchaseStartDate, purchaseEndDate,
                    installationStartDate, installationEndDate, q, pageable
            );
        } else {
            pageResult = equipmentRepository.findWithAdvancedFilters(
                    laboratoryId, parsedStatus, mfr, mdl,
                    calibrationFrequencyDays, purchaseStartDate, purchaseEndDate,
                    installationStartDate, installationEndDate, q, pageable
            );
        }

        return com.calibratesys.dto.PageResponseDto.from(pageResult.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentResponseDto> getEquipmentByLaboratory(Long laboratoryId) {
        if (!laboratoryRepository.existsById(laboratoryId)) {
            throw new ApiException("Laboratory not found with id: " + laboratoryId, HttpStatus.NOT_FOUND);
        }
        dataScopeService.resolveScope(laboratoryId);

        return equipmentRepository.findByLaboratoryId(laboratoryId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EquipmentResponseDto updateEquipment(Long id, EquipmentRequestDto request) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Equipment not found with id: " + id, HttpStatus.NOT_FOUND));

        validateRequest(request);

        Laboratory laboratory = laboratoryRepository.findById(request.getLaboratoryId())
                .orElseThrow(() -> new ApiException("Laboratory not found with id: " + request.getLaboratoryId(), HttpStatus.NOT_FOUND));

        String assetTag = request.getAssetTag().trim();
        if (equipmentRepository.existsByAssetTagAndIdNot(assetTag, id)) {
            throw new ApiException("Equipment with asset tag '" + assetTag + "' already exists", HttpStatus.CONFLICT);
        }

        String serialNumber = StringUtils.hasText(request.getSerialNumber()) ? request.getSerialNumber().trim() : null;
        if (serialNumber != null && equipmentRepository.existsBySerialNumberAndIdNot(serialNumber, id)) {
            throw new ApiException("Equipment with serial number '" + serialNumber + "' already exists", HttpStatus.CONFLICT);
        }

        EquipmentStatus status = parseStatus(request.getStatus());

        String oldAssetTag = equipment.getAssetTag();
        String oldName = equipment.getName();
        String oldStatus = equipment.getStatus().name();
        int oldFreq = equipment.getCalibrationFrequencyDays();

        equipment.setLaboratory(laboratory);
        equipment.setAssetTag(assetTag);
        equipment.setSerialNumber(serialNumber);
        equipment.setName(request.getName().trim());
        equipment.setModelNumber(StringUtils.hasText(request.getModelNumber()) ? request.getModelNumber().trim() : null);
        equipment.setManufacturer(StringUtils.hasText(request.getManufacturer()) ? request.getManufacturer().trim() : null);
        equipment.setPurchaseDate(request.getPurchaseDate());
        equipment.setInstallationDate(request.getInstallationDate());
        equipment.setCalibrationFrequencyDays(request.getCalibrationFrequencyDays());
        equipment.setStatus(status);

        Equipment updated = equipmentRepository.save(equipment);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.EQUIPMENT_UPDATED,
                com.calibratesys.entity.enums.AuditEntityType.EQUIPMENT,
                updated.getId(),
                "Equipment " + updated.getAssetTag() + " (" + updated.getName() + ") was updated.",
                java.util.Map.of("assetTag", oldAssetTag, "name", oldName, "status", oldStatus, "calibrationFrequencyDays", oldFreq),
                java.util.Map.of("assetTag", updated.getAssetTag(), "name", updated.getName(), "status", updated.getStatus().name(), "calibrationFrequencyDays", updated.getCalibrationFrequencyDays())
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Equipment not found with id: " + id, HttpStatus.NOT_FOUND));

        // Enforce delete safety: check if dependent calibration records or schedules exist
        if (calibrationRecordRepository.existsByEquipmentId(id)) {
            throw new ApiException(
                    "Cannot delete equipment because calibration records are associated with it. Mark status as DECOMMISSIONED instead to preserve compliance audit trail.",
                    HttpStatus.CONFLICT
            );
        }
        if (calibrationScheduleRepository.existsByEquipmentId(id)) {
            throw new ApiException(
                    "Cannot delete equipment because calibration schedules are associated with it.",
                    HttpStatus.CONFLICT
            );
        }
        if (calibrationDocumentRepository.existsByEquipmentId(id)) {
            throw new ApiException(
                    "Cannot delete equipment because calibration certificates or documents are associated with it.",
                    HttpStatus.CONFLICT
            );
        }

        equipmentRepository.delete(equipment);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.EQUIPMENT_DELETED,
                com.calibratesys.entity.enums.AuditEntityType.EQUIPMENT,
                id,
                "Equipment " + equipment.getAssetTag() + " (" + equipment.getName() + ") was deleted.",
                java.util.Map.of("assetTag", equipment.getAssetTag(), "name", equipment.getName(), "laboratoryId", equipment.getLaboratory().getId()),
                null
        );
    }

    private void validateRequest(EquipmentRequestDto request) {
        if (request == null) {
            throw new ApiException("Equipment request body is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getLaboratoryId() == null) {
            throw new ApiException("Laboratory ID is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getAssetTag())) {
            throw new ApiException("Asset tag is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ApiException("Equipment name is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getCalibrationFrequencyDays() == null) {
            throw new ApiException("Calibration frequency is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getCalibrationFrequencyDays() <= 0) {
            throw new ApiException("Calibration frequency must be a positive number of days", HttpStatus.BAD_REQUEST);
        }
    }

    private EquipmentStatus parseStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return EquipmentStatus.OPERATIONAL;
        }
        try {
            return EquipmentStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    "Invalid equipment status: '" + statusStr + "'. Valid statuses are: OPERATIONAL, CALIBRATION_DUE, OUT_OF_CALIBRATION, UNDER_MAINTENANCE, DECOMMISSIONED",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private EquipmentResponseDto mapToDto(Equipment equipment) {
        Laboratory lab = equipment.getLaboratory();
        College college = lab != null ? lab.getCollege() : null;

        return new EquipmentResponseDto(
                equipment.getId(),
                lab != null ? lab.getId() : null,
                lab != null ? lab.getLabCode() : null,
                lab != null ? lab.getName() : null,
                college != null ? college.getId() : null,
                college != null ? college.getCode() : null,
                college != null ? college.getName() : null,
                equipment.getAssetTag(),
                equipment.getSerialNumber(),
                equipment.getName(),
                equipment.getModelNumber(),
                equipment.getManufacturer(),
                equipment.getPurchaseDate(),
                equipment.getInstallationDate(),
                equipment.getCalibrationFrequencyDays(),
                equipment.getStatus() != null ? equipment.getStatus().name() : EquipmentStatus.OPERATIONAL.name(),
                equipment.getCreatedAt(),
                equipment.getUpdatedAt()
        );
    }
}
