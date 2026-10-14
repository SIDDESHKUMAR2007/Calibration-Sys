package com.calibratesys.security;

import com.calibratesys.entity.LaboratoryStaff;
import com.calibratesys.entity.Staff;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.LaboratoryRepository;
import com.calibratesys.repository.LaboratoryStaffRepository;
import com.calibratesys.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DataScopeServiceImpl implements DataScopeService {

    private final StaffRepository staffRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;

    @Autowired
    public DataScopeServiceImpl(StaffRepository staffRepository,
                                LaboratoryRepository laboratoryRepository,
                                LaboratoryStaffRepository laboratoryStaffRepository) {
        this.staffRepository = staffRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
    }

    @Override
    public ScopedContext resolveScope(Long requestedLaboratoryId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String username;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }

        Staff staff = staffRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException("Staff user not found", HttpStatus.NOT_FOUND));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (requestedLaboratoryId != null) {
                if (!laboratoryRepository.existsById(requestedLaboratoryId)) {
                    throw new ApiException("Laboratory not found with ID: " + requestedLaboratoryId, HttpStatus.NOT_FOUND);
                }
                return new ScopedContext(staff, true, List.of(requestedLaboratoryId), false);
            }
            return new ScopedContext(staff, true, null, false);
        } else {
            List<LaboratoryStaff> activeAssignments = laboratoryStaffRepository.findActiveAssignmentsByStaffId(staff.getId());
            List<Long> assignedLabIds = activeAssignments.stream()
                    .map(a -> a.getLaboratory().getId())
                    .distinct()
                    .toList();

            if (requestedLaboratoryId != null) {
                if (!assignedLabIds.contains(requestedLaboratoryId)) {
                    throw new ApiException("Access denied: You are not assigned to laboratory ID: " + requestedLaboratoryId, HttpStatus.FORBIDDEN);
                }
                return new ScopedContext(staff, false, List.of(requestedLaboratoryId), false);
            }

            if (assignedLabIds.isEmpty()) {
                return new ScopedContext(staff, false, Collections.emptyList(), true);
            }

            return new ScopedContext(staff, false, assignedLabIds, false);
        }
    }

    @Override
    public ScopedContext resolveScope() {
        return resolveScope(null);
    }
}
