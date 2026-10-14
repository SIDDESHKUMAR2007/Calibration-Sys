package com.calibratesys.controller;

import com.calibratesys.dto.LoginRequestDto;
import com.calibratesys.dto.LoginResponseDto;
import com.calibratesys.dto.StaffProfileDto;
import com.calibratesys.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing authentication endpoints: login, authenticated user profile,
 * and role-restricted verification.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticate staff user by email or staffId with password.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
        LoginResponseDto response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the profile of the currently authenticated staff member.
     */
    @GetMapping("/me")
    public ResponseEntity<StaffProfileDto> getCurrentUser() {
        StaffProfileDto profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * Role-restricted verification endpoint accessible only to ADMIN users.
     */
    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> checkAdminRole() {
        return ResponseEntity.ok("Authorized as ADMIN");
    }
}
