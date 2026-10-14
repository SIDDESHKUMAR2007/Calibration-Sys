package com.calibratesys.service;

import com.calibratesys.dto.LoginRequestDto;
import com.calibratesys.dto.LoginResponseDto;
import com.calibratesys.dto.StaffProfileDto;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates a staff user by email or staffId and returns a JWT token with user details.
     */
    LoginResponseDto login(LoginRequestDto loginRequest);

    /**
     * Retrieves the profile information of the currently authenticated staff member.
     */
    StaffProfileDto getCurrentUserProfile();
}
