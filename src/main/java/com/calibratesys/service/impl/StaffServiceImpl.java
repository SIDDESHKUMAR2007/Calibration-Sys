package com.calibratesys.service.impl;

import com.calibratesys.dto.StaffRequestDto;
import com.calibratesys.dto.StaffResponseDto;
import com.calibratesys.dto.StaffUpdateRequestDto;
import com.calibratesys.entity.LaboratoryStaff;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.LaboratoryStaffRepository;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StaffServiceImpl implements StaffService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9\\s-]{7,25}$");

    private final StaffRepository staffRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.calibratesys.service.AuditService auditService;

    @Autowired
    public StaffServiceImpl(StaffRepository staffRepository,
                            LaboratoryStaffRepository laboratoryStaffRepository,
                            PasswordEncoder passwordEncoder,
                            com.calibratesys.service.AuditService auditService) {
        this.staffRepository = staffRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public StaffResponseDto createStaff(StaffRequestDto request) {
        validateCreateRequest(request);

        String staffId = request.getStaffId().trim();
        if (staffRepository.existsByStaffId(staffId)) {
            throw new ApiException("Staff ID '" + staffId + "' is already in use", HttpStatus.CONFLICT);
        }

        String email = request.getEmail().trim().toLowerCase();
        if (staffRepository.existsByEmail(email)) {
            throw new ApiException("Email address '" + email + "' is already registered", HttpStatus.CONFLICT);
        }

        SystemRole systemRole = parseSystemRole(request.getSystemRole());
        StaffStatus status = parseStaffStatus(request.getStatus());

        Staff staff = new Staff(
                staffId,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null,
                request.getDesignation().trim(),
                request.getDepartment().trim(),
                systemRole,
                status
        );

        Staff saved = staffRepository.save(staff);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.STAFF_CREATED,
                com.calibratesys.entity.enums.AuditEntityType.STAFF,
                saved.getId(),
                "Staff " + saved.getStaffId() + " (" + saved.getFullName() + ") was created.",
                null,
                java.util.Map.of("staffId", saved.getStaffId(), "email", saved.getEmail(), "role", saved.getSystemRole().name(), "status", saved.getStatus().name())
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponseDto getStaffById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ApiException("Staff not found with id: " + id, HttpStatus.NOT_FOUND));
        return mapToDto(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponseDto> getAllStaff(String status, String department) {
        List<Staff> staffList;

        boolean hasStatus = StringUtils.hasText(status);
        boolean hasDepartment = StringUtils.hasText(department);

        if (hasStatus && hasDepartment) {
            StaffStatus parsedStatus = parseStaffStatus(status);
            staffList = staffRepository.findByDepartmentAndStatus(department.trim(), parsedStatus);
        } else if (hasStatus) {
            StaffStatus parsedStatus = parseStaffStatus(status);
            staffList = staffRepository.findByStatus(parsedStatus);
        } else if (hasDepartment) {
            staffList = staffRepository.findByDepartment(department.trim());
        } else {
            staffList = staffRepository.findAll();
        }

        return staffList.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StaffResponseDto updateStaff(Long id, StaffUpdateRequestDto request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ApiException("Staff not found with id: " + id, HttpStatus.NOT_FOUND));

        validateUpdateRequest(request);

        String newStaffId = request.getStaffId().trim();
        if (staffRepository.existsByStaffIdAndIdNot(newStaffId, id)) {
            throw new ApiException("Staff ID '" + newStaffId + "' is already in use by another staff member", HttpStatus.CONFLICT);
        }

        String newEmail = request.getEmail().trim().toLowerCase();
        if (staffRepository.existsByEmailAndIdNot(newEmail, id)) {
            throw new ApiException("Email address '" + newEmail + "' is already registered to another staff member", HttpStatus.CONFLICT);
        }

        SystemRole systemRole = parseSystemRole(request.getSystemRole());
        StaffStatus newStatus = parseStaffStatus(request.getStatus());

        String oldStaffId = staff.getStaffId();
        String oldEmail = staff.getEmail();
        String oldRole = staff.getSystemRole().name();
        String oldStatus = staff.getStatus().name();
        String oldDesignation = staff.getDesignation();

        staff.setStaffId(newStaffId);
        staff.setFirstName(request.getFirstName().trim());
        staff.setLastName(request.getLastName().trim());
        staff.setEmail(newEmail);
        staff.setPhoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null);
        staff.setDesignation(request.getDesignation().trim());
        staff.setDepartment(request.getDepartment().trim());
        staff.setSystemRole(systemRole);

        // If staff is transitioning to INACTIVE status, deactivate their active laboratory assignments
        // and safely remove active primary contact designation to preserve operational safety
        if (newStatus == StaffStatus.INACTIVE && staff.getStatus() != StaffStatus.INACTIVE) {
            List<LaboratoryStaff> assignments = laboratoryStaffRepository.findByStaffId(id);
            for (LaboratoryStaff assignment : assignments) {
                if ("ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
                    assignment.setStatus("INACTIVE");
                }
                if (assignment.isPrimaryContact()) {
                    assignment.setPrimaryContact(false);
                }
                laboratoryStaffRepository.save(assignment);
            }
        }
        staff.setStatus(newStatus);

        // Optional password update
        if (StringUtils.hasText(request.getPassword())) {
            if (request.getPassword().trim().length() < 6) {
                throw new ApiException("Password must be at least 6 characters long", HttpStatus.BAD_REQUEST);
            }
            staff.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }

        Staff updated = staffRepository.save(staff);

        auditService.logEvent(
                newStatus == StaffStatus.INACTIVE && "ACTIVE".equals(oldStatus) ? com.calibratesys.entity.enums.AuditAction.STAFF_DEACTIVATED : com.calibratesys.entity.enums.AuditAction.STAFF_UPDATED,
                com.calibratesys.entity.enums.AuditEntityType.STAFF,
                updated.getId(),
                "Staff " + updated.getStaffId() + " was updated.",
                java.util.Map.of("staffId", oldStaffId, "email", oldEmail, "role", oldRole, "status", oldStatus, "designation", oldDesignation),
                java.util.Map.of("staffId", updated.getStaffId(), "email", updated.getEmail(), "role", updated.getSystemRole().name(), "status", updated.getStatus().name(), "designation", updated.getDesignation())
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ApiException("Staff not found with id: " + id, HttpStatus.NOT_FOUND));

        // Enforce delete safety: check if laboratory assignments exist
        if (laboratoryStaffRepository.existsByStaffId(id)) {
            throw new ApiException(
                    "Cannot delete staff member with existing laboratory assignments. Deactivate the staff member instead to preserve historical records.",
                    HttpStatus.CONFLICT
            );
        }

        staffRepository.delete(staff);

        auditService.logEvent(
                com.calibratesys.entity.enums.AuditAction.STAFF_DELETED,
                com.calibratesys.entity.enums.AuditEntityType.STAFF,
                id,
                "Staff " + staff.getStaffId() + " was deleted.",
                java.util.Map.of("staffId", staff.getStaffId(), "email", staff.getEmail()),
                null
        );
    }

    private void validateCreateRequest(StaffRequestDto request) {
        if (request == null) {
            throw new ApiException("Staff request body is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getStaffId())) {
            throw new ApiException("Staff ID is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getFirstName())) {
            throw new ApiException("First name is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getLastName())) {
            throw new ApiException("Last name is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ApiException("Email is required", HttpStatus.BAD_REQUEST);
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new ApiException("Email format is invalid: " + request.getEmail(), HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            if (!PHONE_PATTERN.matcher(request.getPhoneNumber().trim()).matches()) {
                throw new ApiException("Phone number format is invalid: " + request.getPhoneNumber(), HttpStatus.BAD_REQUEST);
            }
        }
        if (!StringUtils.hasText(request.getDesignation())) {
            throw new ApiException("Designation is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getDepartment())) {
            throw new ApiException("Department is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getPassword()) || request.getPassword().trim().length() < 6) {
            throw new ApiException("Password must be at least 6 characters long", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateUpdateRequest(StaffUpdateRequestDto request) {
        if (request == null) {
            throw new ApiException("Staff request body is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getStaffId())) {
            throw new ApiException("Staff ID is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getFirstName())) {
            throw new ApiException("First name is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getLastName())) {
            throw new ApiException("Last name is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new ApiException("Email is required", HttpStatus.BAD_REQUEST);
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new ApiException("Email format is invalid: " + request.getEmail(), HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            if (!PHONE_PATTERN.matcher(request.getPhoneNumber().trim()).matches()) {
                throw new ApiException("Phone number format is invalid: " + request.getPhoneNumber(), HttpStatus.BAD_REQUEST);
            }
        }
        if (!StringUtils.hasText(request.getDesignation())) {
            throw new ApiException("Designation is required", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(request.getDepartment())) {
            throw new ApiException("Department is required", HttpStatus.BAD_REQUEST);
        }
    }

    private SystemRole parseSystemRole(String roleStr) {
        if (!StringUtils.hasText(roleStr)) {
            return SystemRole.LAB_STAFF;
        }
        try {
            return SystemRole.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid system role: " + roleStr + ". Valid roles are: ADMIN, LAB_STAFF", HttpStatus.BAD_REQUEST);
        }
    }

    private StaffStatus parseStaffStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return StaffStatus.ACTIVE;
        }
        try {
            return StaffStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid staff status: " + statusStr + ". Valid statuses are: ACTIVE, INACTIVE", HttpStatus.BAD_REQUEST);
        }
    }

    private StaffResponseDto mapToDto(Staff staff) {
        int labCount = staff.getLaboratoryAssignments() != null ? staff.getLaboratoryAssignments().size() : 0;
        return new StaffResponseDto(
                staff.getId(),
                staff.getStaffId(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getFullName(),
                staff.getEmail(),
                staff.getPhoneNumber(),
                staff.getDesignation(),
                staff.getDepartment(),
                staff.getSystemRole().name(),
                staff.getStatus().name(),
                labCount,
                staff.getLastLoginAt(),
                staff.getCreatedAt(),
                staff.getUpdatedAt()
        );
    }

    private static final Map<String, String> STAFF_SORT_FIELDS = Map.of(
            "firstName", "firstName",
            "lastName", "lastName",
            "staffId", "staffId",
            "department", "department",
            "designation", "designation",
            "status", "status",
            "createdAt", "createdAt"
    );

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponseDto> searchStaff(String status, String department, String systemRole, Long laboratoryId, String roleInLab, String search) {
        StaffStatus parsedStatus = StringUtils.hasText(status) ? parseStaffStatus(status) : null;
        com.calibratesys.entity.enums.SystemRole parsedRole = null;
        if (StringUtils.hasText(systemRole)) {
            try {
                parsedRole = com.calibratesys.entity.enums.SystemRole.valueOf(systemRole.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid system role: " + systemRole, HttpStatus.BAD_REQUEST);
            }
        }
        com.calibratesys.entity.enums.StaffLabRole parsedLabRole = null;
        if (StringUtils.hasText(roleInLab)) {
            try {
                parsedLabRole = com.calibratesys.entity.enums.StaffLabRole.valueOf(roleInLab.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid role in lab: " + roleInLab, HttpStatus.BAD_REQUEST);
            }
        }

        String dept = StringUtils.hasText(department) ? department.trim() : null;
        String q = StringUtils.hasText(search) ? search.trim() : null;

        List<Staff> staffList = staffRepository.findWithAdvancedFilters(parsedStatus, parsedRole, dept, laboratoryId, parsedLabRole, q);
        return staffList.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public com.calibratesys.dto.PageResponseDto<StaffResponseDto> searchStaffPaginated(
            String status, String department, String systemRole, Long laboratoryId, String roleInLab, String search,
            String sortBy, String sortDirection, int page, int size
    ) {
        org.springframework.data.domain.Sort sort = com.calibratesys.util.SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "firstName", org.springframework.data.domain.Sort.Direction.ASC, STAFF_SORT_FIELDS
        );

        List<StaffResponseDto> all = searchStaff(status, department, systemRole, laboratoryId, roleInLab, search);

        java.util.Comparator<StaffResponseDto> comparator = switch (sort.iterator().next().getProperty()) {
            case "lastName" -> java.util.Comparator.comparing(StaffResponseDto::getLastName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "staffId" -> java.util.Comparator.comparing(StaffResponseDto::getStaffId, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "department" -> java.util.Comparator.comparing(StaffResponseDto::getDepartment, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "designation" -> java.util.Comparator.comparing(StaffResponseDto::getDesignation, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "status" -> java.util.Comparator.comparing(StaffResponseDto::getStatus, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "createdAt" -> java.util.Comparator.comparing(StaffResponseDto::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            default -> java.util.Comparator.comparing(StaffResponseDto::getFirstName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };

        if (sort.iterator().next().isDescending()) {
            comparator = comparator.reversed();
        }
        all.sort(comparator);

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        int fromIndex = Math.min(validatedPage * validatedSize, all.size());
        int toIndex = Math.min(fromIndex + validatedSize, all.size());

        List<StaffResponseDto> pageContent = all.subList(fromIndex, toIndex);
        org.springframework.data.domain.Page<StaffResponseDto> springPage =
                new org.springframework.data.domain.PageImpl<>(pageContent, org.springframework.data.domain.PageRequest.of(validatedPage, validatedSize, sort), all.size());

        return com.calibratesys.dto.PageResponseDto.from(springPage);
    }
}
