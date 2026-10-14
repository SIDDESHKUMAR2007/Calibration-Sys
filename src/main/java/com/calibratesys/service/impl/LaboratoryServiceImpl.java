package com.calibratesys.service.impl;

import com.calibratesys.dto.LaboratoryRequestDto;
import com.calibratesys.dto.LaboratoryResponseDto;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.enums.LabStatus;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.CollegeRepository;
import com.calibratesys.repository.EquipmentRepository;
import com.calibratesys.repository.LaboratoryRepository;
import com.calibratesys.repository.LaboratoryStaffRepository;
import com.calibratesys.service.LaboratoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LaboratoryServiceImpl implements LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;
    private final CollegeRepository collegeRepository;
    private final EquipmentRepository equipmentRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final com.calibratesys.service.AuditService auditService;
    private final com.calibratesys.security.DataScopeService dataScopeService;

    @Autowired
    public LaboratoryServiceImpl(LaboratoryRepository laboratoryRepository,
                                 CollegeRepository collegeRepository,
                                 EquipmentRepository equipmentRepository,
                                 LaboratoryStaffRepository laboratoryStaffRepository,
                                 com.calibratesys.service.AuditService auditService,
                                 com.calibratesys.security.DataScopeService dataScopeService) {
        this.laboratoryRepository = laboratoryRepository;
        this.collegeRepository = collegeRepository;
        this.equipmentRepository = equipmentRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.auditService = auditService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    @Transactional
    public LaboratoryResponseDto createLaboratory(LaboratoryRequestDto request) {
        validateRequest(request);

        College college = collegeRepository.findById(request.getCollegeId())
                .orElseThrow(() -> new ApiException("College not found with id: " + request.getCollegeId(), HttpStatus.NOT_FOUND));

        String labCode = request.getLabCode().trim();
        if (laboratoryRepository.existsByLabCode(labCode)) {
            throw new ApiException("Laboratory with code '" + labCode + "' already exists", HttpStatus.CONFLICT);
        }

        LabStatus status = parseStatus(request.getStatus());

        Laboratory laboratory = new Laboratory(
                college,
                labCode,
                request.getName().trim(),
                request.getDepartment().trim(),
                StringUtils.hasText(request.getLocation()) ? request.getLocation().trim() : null,
                status
        );

        Laboratory saved = laboratoryRepository.save(laboratory);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.LABORATORY_CREATED,
                com.calibratesys.entity.enums.AuditEntityType.LABORATORY,
                saved.getId(),
                "Laboratory " + saved.getLabCode() + " was created.",
                null,
                java.util.Map.of("labCode", saved.getLabCode(), "name", saved.getName(), "department", saved.getDepartment(), "status", saved.getStatus().name())
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LaboratoryResponseDto getLaboratoryById(Long id) {
        Laboratory laboratory = laboratoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("Laboratory not found with id: " + id, HttpStatus.NOT_FOUND));
        return mapToDto(laboratory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryResponseDto> getLaboratories(Long collegeId, String statusStr) {
        LabStatus status = null;
        if (StringUtils.hasText(statusStr)) {
            status = parseStatus(statusStr);
        }

        List<Laboratory> laboratories;
        if (collegeId != null && status != null) {
            laboratories = laboratoryRepository.findByCollegeIdAndStatus(collegeId, status);
        } else if (collegeId != null) {
            laboratories = laboratoryRepository.findByCollegeId(collegeId);
        } else if (status != null) {
            laboratories = laboratoryRepository.findByStatus(status);
        } else {
            laboratories = laboratoryRepository.findAll();
        }

        return laboratories.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LaboratoryResponseDto updateLaboratory(Long id, LaboratoryRequestDto request) {
        Laboratory laboratory = laboratoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("Laboratory not found with id: " + id, HttpStatus.NOT_FOUND));

        validateRequest(request);

        College college = collegeRepository.findById(request.getCollegeId())
                .orElseThrow(() -> new ApiException("College not found with id: " + request.getCollegeId(), HttpStatus.NOT_FOUND));

        String newLabCode = request.getLabCode().trim();
        if (laboratoryRepository.existsByLabCodeAndIdNot(newLabCode, id)) {
            throw new ApiException("Laboratory with code '" + newLabCode + "' already exists", HttpStatus.CONFLICT);
        }

        LabStatus status = parseStatus(request.getStatus());

        String oldCode = laboratory.getLabCode();
        String oldName = laboratory.getName();
        String oldDept = laboratory.getDepartment();
        String oldStatus = laboratory.getStatus().name();

        laboratory.setCollege(college);
        laboratory.setLabCode(newLabCode);
        laboratory.setName(request.getName().trim());
        laboratory.setDepartment(request.getDepartment().trim());
        laboratory.setLocation(StringUtils.hasText(request.getLocation()) ? request.getLocation().trim() : null);
        laboratory.setStatus(status);

        Laboratory updated = laboratoryRepository.save(laboratory);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.LABORATORY_UPDATED,
                com.calibratesys.entity.enums.AuditEntityType.LABORATORY,
                updated.getId(),
                "Laboratory " + updated.getLabCode() + " was updated.",
                java.util.Map.of("labCode", oldCode, "name", oldName, "department", oldDept, "status", oldStatus),
                java.util.Map.of("labCode", updated.getLabCode(), "name", updated.getName(), "department", updated.getDepartment(), "status", updated.getStatus().name())
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteLaboratory(Long id) {
        Laboratory laboratory = laboratoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("Laboratory not found with id: " + id, HttpStatus.NOT_FOUND));

        // Enforce delete safety: check if equipment is associated
        if (equipmentRepository.existsByLaboratoryId(id)) {
            throw new ApiException(
                    "Cannot delete laboratory because equipment items are registered under it. Reassign or remove equipment first.",
                    HttpStatus.CONFLICT
            );
        }

        // Enforce delete safety: check if staff are assigned
        if (laboratoryStaffRepository.existsByLaboratoryId(id)) {
            throw new ApiException(
                    "Cannot delete laboratory because staff members are assigned to it. Remove staff assignments first.",
                    HttpStatus.CONFLICT
            );
        }

        laboratoryRepository.delete(laboratory);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.LABORATORY_DELETED,
                com.calibratesys.entity.enums.AuditEntityType.LABORATORY,
                id,
                "Laboratory " + laboratory.getLabCode() + " was deleted.",
                java.util.Map.of("labCode", laboratory.getLabCode(), "name", laboratory.getName()),
                null
        );
    }

    private void validateRequest(LaboratoryRequestDto request) {
        if (request == null) {
            throw new ApiException("Laboratory request body is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getCollegeId() == null) {
            throw new ApiException("College ID is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getLabCode())) {
            throw new ApiException("Laboratory code is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new ApiException("Laboratory name is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getDepartment())) {
            throw new ApiException("Department is required", HttpStatus.BAD_REQUEST);
        }
    }

    private LabStatus parseStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return LabStatus.ACTIVE;
        }
        try {
            return LabStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    "Invalid laboratory status: '" + statusStr + "'. Valid values are: ACTIVE, INACTIVE, UNDER_MAINTENANCE",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private LaboratoryResponseDto mapToDto(Laboratory lab) {
        return new LaboratoryResponseDto(
                lab.getId(),
                lab.getCollege().getId(),
                lab.getCollege().getCode(),
                lab.getCollege().getName(),
                lab.getLabCode(),
                lab.getName(),
                lab.getDepartment(),
                lab.getLocation(),
                lab.getStatus().name(),
                lab.getCreatedAt(),
                lab.getUpdatedAt()
        );
    }

    private static final Map<String, String> LAB_SORT_FIELDS = Map.of(
            "name", "name",
            "labCode", "labCode",
            "department", "department",
            "status", "status",
            "createdAt", "createdAt"
    );

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryResponseDto> searchLaboratories(Long collegeId, String status, String department, String search) {
        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope(null);
        if (scope.isEmptyScope()) {
            return java.util.Collections.emptyList();
        }

        LabStatus parsedStatus = StringUtils.hasText(status) ? parseStatus(status) : null;
        String dept = StringUtils.hasText(department) ? department.trim() : null;
        String q = StringUtils.hasText(search) ? search.trim() : null;

        List<Laboratory> labs;
        if (scope.laboratoryIds() != null) {
            labs = laboratoryRepository.findWithAdvancedFiltersAndLaboratoryIds(scope.laboratoryIds(), collegeId, parsedStatus, dept, q);
        } else {
            labs = laboratoryRepository.findWithAdvancedFilters(collegeId, parsedStatus, dept, q);
        }

        return labs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public com.calibratesys.dto.PageResponseDto<LaboratoryResponseDto> searchLaboratoriesPaginated(
            Long collegeId, String status, String department, String search,
            String sortBy, String sortDirection, int page, int size
    ) {
        org.springframework.data.domain.Sort sort = com.calibratesys.util.SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "name", org.springframework.data.domain.Sort.Direction.ASC, LAB_SORT_FIELDS
        );

        List<LaboratoryResponseDto> all = searchLaboratories(collegeId, status, department, search);

        // In-memory sort based on validated sort
        java.util.Comparator<LaboratoryResponseDto> comparator = switch (sort.iterator().next().getProperty()) {
            case "labCode" -> java.util.Comparator.comparing(LaboratoryResponseDto::getLabCode, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "department" -> java.util.Comparator.comparing(LaboratoryResponseDto::getDepartment, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "status" -> java.util.Comparator.comparing(LaboratoryResponseDto::getStatus, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "createdAt" -> java.util.Comparator.comparing(LaboratoryResponseDto::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            default -> java.util.Comparator.comparing(LaboratoryResponseDto::getName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };

        if (sort.iterator().next().isDescending()) {
            comparator = comparator.reversed();
        }
        all.sort(comparator);

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        int fromIndex = Math.min(validatedPage * validatedSize, all.size());
        int toIndex = Math.min(fromIndex + validatedSize, all.size());

        List<LaboratoryResponseDto> pageContent = all.subList(fromIndex, toIndex);
        org.springframework.data.domain.Page<LaboratoryResponseDto> springPage =
                new org.springframework.data.domain.PageImpl<>(pageContent, org.springframework.data.domain.PageRequest.of(validatedPage, validatedSize, sort), all.size());

        return com.calibratesys.dto.PageResponseDto.from(springPage);
    }
}
