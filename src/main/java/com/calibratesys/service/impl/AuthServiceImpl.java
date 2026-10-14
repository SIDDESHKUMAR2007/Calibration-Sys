package com.calibratesys.service.impl;

import com.calibratesys.dto.LoginRequestDto;
import com.calibratesys.dto.LoginResponseDto;
import com.calibratesys.dto.StaffProfileDto;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.security.JwtTokenProvider;
import com.calibratesys.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final StaffRepository staffRepository;
    private final JwtTokenProvider tokenProvider;
    private final com.calibratesys.service.AuditService auditService;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           StaffRepository staffRepository,
                           JwtTokenProvider tokenProvider,
                           com.calibratesys.service.AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.staffRepository = staffRepository;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        if (!StringUtils.hasText(loginRequest.getIdentifier()) || !StringUtils.hasText(loginRequest.getPassword())) {
            throw new ApiException("Email/Staff ID and password must not be empty", HttpStatus.BAD_REQUEST);
        }

        Staff staff = null;
        try {
            // Verify staff exists and check active status before attempting authentication
            staff = staffRepository.findByEmailOrStaffId(loginRequest.getIdentifier().trim(), loginRequest.getIdentifier().trim())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email/staff ID or password"));

            if (staff.getStatus() != StaffStatus.ACTIVE) {
                throw new DisabledException("Staff account is inactive. Please contact the system administrator.");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            staff.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Update last login timestamp
            staff.setLastLoginAt(LocalDateTime.now());
            staffRepository.save(staff);

            String jwt = tokenProvider.generateToken(authentication);

            auditService.recordAuthEvent(
                    com.calibratesys.entity.enums.AuditAction.LOGIN_SUCCESS,
                    staff,
                    "Successful login for staff " + staff.getStaffId(),
                    staff.getEmail()
            );

            return new LoginResponseDto(
                    jwt,
                    tokenProvider.getExpirationMs(),
                    staff.getStaffId(),
                    staff.getFullName(),
                    staff.getEmail(),
                    staff.getSystemRole().name()
            );
        } catch (Exception ex) {
            auditService.recordAuthEvent(
                    com.calibratesys.entity.enums.AuditAction.LOGIN_FAILURE,
                    staff,
                    "Failed login attempt for identifier: " + loginRequest.getIdentifier(),
                    loginRequest.getIdentifier()
            );
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StaffProfileDto getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ApiException("User is not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String username;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }

        Staff staff = staffRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException("Staff user not found", HttpStatus.NOT_FOUND));

        return new StaffProfileDto(
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
                staff.getLastLoginAt()
        );
    }
}
