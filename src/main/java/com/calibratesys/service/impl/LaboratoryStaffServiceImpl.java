package com.calibratesys.service.impl;

import com.calibratesys.dto.LaboratoryStaffRequestDto;
import com.calibratesys.dto.LaboratoryStaffResponseDto;
import com.calibratesys.dto.LaboratoryStaffUpdateDto;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.LaboratoryStaff;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffLabRole;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.LaboratoryRepository;
import com.calibratesys.repository.LaboratoryStaffRepository;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.service.LaboratoryStaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LaboratoryStaffServiceImpl implements LaboratoryStaffService {

    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final StaffRepository staffRepository;
    private final com.calibratesys.service.AuditService auditService;
    private final com.calibratesys.security.DataScopeService dataScopeService;

    @Autowired
    public LaboratoryStaffServiceImpl(LaboratoryStaffRepository laboratoryStaffRepository,
                                      LaboratoryRepository laboratoryRepository,
                                      StaffRepository staffRepository,
                                      com.calibratesys.service.AuditService auditService,
                                      com.calibratesys.security.DataScopeService dataScopeService) {
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.staffRepository = staffRepository;
        this.auditService = auditService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryStaffResponseDto> getStaffByLaboratory(Long laboratoryId) {
        if (!laboratoryRepository.existsById(laboratoryId)) {
            throw new ApiException("Laboratory not found with id: " + laboratoryId, HttpStatus.NOT_FOUND);
        }
        dataScopeService.resolveScope(laboratoryId);

        return laboratoryStaffRepository.findByLaboratoryId(laboratoryId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryStaffResponseDto> getLaboratoriesByStaff(Long staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new ApiException("Staff not found with id: " + staffId, HttpStatus.NOT_FOUND);
        }

        return laboratoryStaffRepository.findByStaffId(staffId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LaboratoryStaffResponseDto assignStaffToLaboratory(Long laboratoryId, LaboratoryStaffRequestDto request) {
        validateRequest(request);

        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new ApiException("Laboratory not found with id: " + laboratoryId, HttpStatus.NOT_FOUND));

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ApiException("Staff not found with id: " + request.getStaffId(), HttpStatus.NOT_FOUND));

        if (laboratoryStaffRepository.existsByLaboratoryIdAndStaffId(laboratoryId, request.getStaffId())) {
            throw new ApiException(
                    "Staff member '" + staff.getFullName() + "' is already assigned to laboratory '" + laboratory.getName() +
                            "'. Use PUT /api/laboratories/" + laboratoryId + "/staff/" + request.getStaffId() + " to update.",
                    HttpStatus.CONFLICT
            );
        }

        StaffLabRole roleInLab = parseStaffLabRole(request.getRoleInLab());
        String assignmentStatus = parseAssignmentStatus(request.getStatus());
        boolean isPrimaryContact = Boolean.TRUE.equals(request.getIsPrimaryContact());

        // Inactive staff restrictions
        if (staff.getStatus() == StaffStatus.INACTIVE) {
            if ("ACTIVE".equalsIgnoreCase(assignmentStatus)) {
                throw new ApiException("Inactive staff member cannot be given an ACTIVE laboratory assignment", HttpStatus.BAD_REQUEST);
            }
            if (isPrimaryContact) {
                throw new ApiException("Inactive staff member cannot be assigned as primary contact", HttpStatus.BAD_REQUEST);
            }
        }

        // Inactive assignment cannot be primary contact
        if ("INACTIVE".equalsIgnoreCase(assignmentStatus) && isPrimaryContact) {
            throw new ApiException("An inactive assignment cannot be designated as primary contact", HttpStatus.BAD_REQUEST);
        }

        // Primary contact rule: A laboratory has at most one active primary contact.
        // Automatically demote the previous active primary contact to ensure seamless responsibility handover.
        if (isPrimaryContact && "ACTIVE".equalsIgnoreCase(assignmentStatus)) {
            List<LaboratoryStaff> existingPrimaryContacts = laboratoryStaffRepository
                    .findByLaboratoryIdAndIsPrimaryContactTrue(laboratoryId);
            for (LaboratoryStaff existing : existingPrimaryContacts) {
                existing.setPrimaryContact(false);
                laboratoryStaffRepository.save(existing);
            }
        }

        LaboratoryStaff assignment = new LaboratoryStaff(laboratory, staff, roleInLab, isPrimaryContact);
        assignment.setStatus(assignmentStatus);
        assignment.setAssignedAt(LocalDateTime.now());

        LaboratoryStaff saved = laboratoryStaffRepository.save(assignment);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.STAFF_ASSIGNED_TO_LAB,
                com.calibratesys.entity.enums.AuditEntityType.LABORATORY_STAFF,
                saved.getId(),
                "Staff " + staff.getStaffId() + " (" + staff.getFullName() + ") was assigned to laboratory " + laboratory.getLabCode() + " as " + roleInLab.name() + ".",
                null,
                java.util.Map.of("laboratoryId", laboratory.getId(), "laboratoryCode", laboratory.getLabCode(), "staffId", staff.getId(), "staffCode", staff.getStaffId(), "roleInLab", roleInLab.name(), "isPrimaryContact", isPrimaryContact, "status", assignmentStatus)
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public LaboratoryStaffResponseDto updateStaffAssignment(Long laboratoryId, Long staffId, LaboratoryStaffUpdateDto request) {
        validateUpdateRequest(request);

        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .orElseThrow(() -> new ApiException("Laboratory not found with id: " + laboratoryId, HttpStatus.NOT_FOUND));

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ApiException("Staff not found with id: " + staffId, HttpStatus.NOT_FOUND));

        LaboratoryStaff assignment = laboratoryStaffRepository.findByLaboratoryIdAndStaffId(laboratoryId, staffId)
                .orElseThrow(() -> new ApiException(
                        "Assignment not found for staff id " + staffId + " in laboratory id " + laboratoryId,
                        HttpStatus.NOT_FOUND
                ));

        StaffLabRole roleInLab = parseStaffLabRole(request.getRoleInLab());
        String assignmentStatus = parseAssignmentStatus(request.getStatus());
        boolean isPrimaryContact = Boolean.TRUE.equals(request.getIsPrimaryContact());

        // Inactive staff restrictions
        if (staff.getStatus() == StaffStatus.INACTIVE) {
            if ("ACTIVE".equalsIgnoreCase(assignmentStatus)) {
                throw new ApiException("Cannot activate assignment for an inactive staff member", HttpStatus.BAD_REQUEST);
            }
            if (isPrimaryContact) {
                throw new ApiException("Cannot set inactive staff member as primary contact", HttpStatus.BAD_REQUEST);
            }
        }

        // Inactive assignment cannot be primary contact
        if ("INACTIVE".equalsIgnoreCase(assignmentStatus) && isPrimaryContact) {
            throw new ApiException("An inactive assignment cannot be designated as primary contact", HttpStatus.BAD_REQUEST);
        }

        // Primary contact rule: If designating as active primary contact, demote any other existing primary contact
        if (isPrimaryContact && "ACTIVE".equalsIgnoreCase(assignmentStatus)) {
            List<LaboratoryStaff> existingPrimaryContacts = laboratoryStaffRepository
                    .findByLaboratoryIdAndIsPrimaryContactTrue(laboratoryId);
            for (LaboratoryStaff existing : existingPrimaryContacts) {
                if (!existing.getId().equals(assignment.getId())) {
                    existing.setPrimaryContact(false);
                    laboratoryStaffRepository.save(existing);
                }
            }
        }

        String oldRole = assignment.getRoleInLab().name();
        String oldStatus = assignment.getStatus();
        boolean oldPrimary = assignment.isPrimaryContact();

        assignment.setRoleInLab(roleInLab);
        assignment.setStatus(assignmentStatus);
        assignment.setPrimaryContact(isPrimaryContact);

        LaboratoryStaff saved = laboratoryStaffRepository.save(assignment);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.STAFF_LAB_ASSIGNMENT_UPDATED,
                com.calibratesys.entity.enums.AuditEntityType.LABORATORY_STAFF,
                saved.getId(),
                "Assignment for staff " + staff.getStaffId() + " in laboratory " + laboratory.getLabCode() + " was updated.",
                java.util.Map.of("roleInLab", oldRole, "status", oldStatus, "isPrimaryContact", oldPrimary),
                java.util.Map.of("roleInLab", roleInLab.name(), "status", assignmentStatus, "isPrimaryContact", isPrimaryContact)
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void removeStaffAssignment(Long laboratoryId, Long staffId) {
        if (!laboratoryRepository.existsById(laboratoryId)) {
            throw new ApiException("Laboratory not found with id: " + laboratoryId, HttpStatus.NOT_FOUND);
        }
        if (!staffRepository.existsById(staffId)) {
            throw new ApiException("Staff not found with id: " + staffId, HttpStatus.NOT_FOUND);
        }

        LaboratoryStaff assignment = laboratoryStaffRepository.findByLaboratoryIdAndStaffId(laboratoryId, staffId)
                .orElseThrow(() -> new ApiException(
                        "Assignment not found for staff id " + staffId + " in laboratory id " + laboratoryId,
                        HttpStatus.NOT_FOUND
                ));

        laboratoryStaffRepository.delete(assignment);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.STAFF_UNASSIGNED_FROM_LAB,
                com.calibratesys.entity.enums.AuditEntityType.LABORATORY_STAFF,
                assignment.getId(),
                "Staff " + assignment.getStaff().getStaffId() + " was unassigned from laboratory " + assignment.getLaboratory().getLabCode() + ".",
                java.util.Map.of("laboratoryId", laboratoryId, "staffId", staffId, "roleInLab", assignment.getRoleInLab().name()),
                null
        );
    }

    private void validateRequest(LaboratoryStaffRequestDto request) {
        if (request == null) {
            throw new ApiException("Assignment request body is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getStaffId() == null) {
            throw new ApiException("Staff ID is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getRoleInLab())) {
            throw new ApiException("Role in laboratory is required", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateUpdateRequest(LaboratoryStaffUpdateDto request) {
        if (request == null) {
            throw new ApiException("Assignment update body is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getRoleInLab())) {
            throw new ApiException("Role in laboratory is required", HttpStatus.BAD_REQUEST);
        }
    }

    private StaffLabRole parseStaffLabRole(String roleStr) {
        if (!StringUtils.hasText(roleStr)) {
            return StaffLabRole.STAFF;
        }
        try {
            return StaffLabRole.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    "Invalid laboratory role: '" + roleStr + "'. Valid roles are: PRIMARY_INCHARGE, LAB_TECHNICIAN, SAFETY_OFFICER, CALIBRATION_COORDINATOR, STAFF",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String parseAssignmentStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return "ACTIVE";
        }
        String upper = statusStr.trim().toUpperCase();
        if ("ACTIVE".equals(upper) || "INACTIVE".equals(upper)) {
            return upper;
        }
        throw new ApiException("Invalid assignment status: '" + statusStr + "'. Valid values are: ACTIVE, INACTIVE", HttpStatus.BAD_REQUEST);
    }

    private LaboratoryStaffResponseDto mapToDto(LaboratoryStaff assignment) {
        Laboratory lab = assignment.getLaboratory();
        Staff staff = assignment.getStaff();

        return new LaboratoryStaffResponseDto(
                assignment.getId(),
                lab.getId(),
                lab.getName(),
                lab.getLabCode(),
                staff.getId(),
                staff.getStaffId(),
                staff.getFullName(),
                staff.getEmail(),
                staff.getPhoneNumber(),
                staff.getDepartment(),
                staff.getDesignation(),
                staff.getStatus().name(),
                assignment.getRoleInLab().name(),
                assignment.isPrimaryContact(),
                assignment.getStatus(),
                assignment.getAssignedAt()
        );
    }
}
