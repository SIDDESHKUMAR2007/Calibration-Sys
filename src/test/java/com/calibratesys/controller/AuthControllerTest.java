package com.calibratesys.controller;

import com.calibratesys.dto.LoginRequestDto;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private Staff activeLabStaff;
    private Staff inactiveLabStaff;
    private Staff adminStaff;

    @BeforeEach
    void setUp() {
        // Create or get active lab staff
        Optional<Staff> existingLabStaff = staffRepository.findByEmail("tech.sarah@calibratesys.edu");
        if (existingLabStaff.isEmpty()) {
            activeLabStaff = new Staff(
                    "TECH-001",
                    "Sarah",
                    "Connor",
                    "tech.sarah@calibratesys.edu",
                    passwordEncoder.encode("SecretPass@123"),
                    "555-0201",
                    "Calibration Specialist",
                    "Physics Lab",
                    SystemRole.LAB_STAFF,
                    StaffStatus.ACTIVE
            );
            activeLabStaff = staffRepository.save(activeLabStaff);
        } else {
            activeLabStaff = existingLabStaff.get();
            activeLabStaff.setPasswordHash(passwordEncoder.encode("SecretPass@123"));
            activeLabStaff.setStatus(StaffStatus.ACTIVE);
            activeLabStaff = staffRepository.save(activeLabStaff);
        }

        // Create or get inactive lab staff
        Optional<Staff> existingInactive = staffRepository.findByEmail("inactive.john@calibratesys.edu");
        if (existingInactive.isEmpty()) {
            inactiveLabStaff = new Staff(
                    "INACT-001",
                    "John",
                    "Doe",
                    "inactive.john@calibratesys.edu",
                    passwordEncoder.encode("SecretPass@123"),
                    "555-0202",
                    "Former Assistant",
                    "Biology Lab",
                    SystemRole.LAB_STAFF,
                    StaffStatus.INACTIVE
            );
            inactiveLabStaff = staffRepository.save(inactiveLabStaff);
        } else {
            inactiveLabStaff = existingInactive.get();
            inactiveLabStaff.setPasswordHash(passwordEncoder.encode("SecretPass@123"));
            inactiveLabStaff.setStatus(StaffStatus.INACTIVE);
            inactiveLabStaff = staffRepository.save(inactiveLabStaff);
        }

        // Get admin created by DataInitializer or create
        Optional<Staff> existingAdmin = staffRepository.findByEmail("admin@calibratesys.edu");
        if (existingAdmin.isPresent()) {
            adminStaff = existingAdmin.get();
        } else {
            adminStaff = new Staff(
                    "ADMIN-001",
                    "System",
                    "Admin",
                    "admin@calibratesys.edu",
                    passwordEncoder.encode("Admin@12345"),
                    "555-0199",
                    "System Administrator",
                    "Administration",
                    SystemRole.ADMIN,
                    StaffStatus.ACTIVE
            );
            adminStaff = staffRepository.save(adminStaff);
        }
    }

    @Test
    @DisplayName("1. Successful login with email returns JWT token and user info without password hash")
    void testSuccessfulLoginWithEmail() throws Exception {
        LoginRequestDto request = new LoginRequestDto("tech.sarah@calibratesys.edu", "SecretPass@123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.staffId").value("TECH-001"))
                .andExpect(jsonPath("$.email").value("tech.sarah@calibratesys.edu"))
                .andExpect(jsonPath("$.role").value("LAB_STAFF"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(responseJson.get("token").asText().length() > 20);
    }

    @Test
    @DisplayName("2. Successful login with staffId identifier")
    void testSuccessfulLoginWithStaffId() throws Exception {
        LoginRequestDto request = new LoginRequestDto("TECH-001", "SecretPass@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.staffId").value("TECH-001"))
                .andExpect(jsonPath("$.email").value("tech.sarah@calibratesys.edu"));
    }

    @Test
    @DisplayName("3. Invalid password returns HTTP 401 Unauthorized")
    void testLoginWithInvalidPassword() throws Exception {
        LoginRequestDto request = new LoginRequestDto("tech.sarah@calibratesys.edu", "WrongPassword999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email/staff ID or password"));
    }

    @Test
    @DisplayName("4. Unknown user returns HTTP 401 Unauthorized")
    void testLoginWithUnknownUser() throws Exception {
        LoginRequestDto request = new LoginRequestDto("nonexistent@calibratesys.edu", "SomePassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email/staff ID or password"));
    }

    @Test
    @DisplayName("5. Inactive staff account returns HTTP 403 Forbidden")
    void testLoginWithInactiveAccount() throws Exception {
        LoginRequestDto request = new LoginRequestDto("inactive.john@calibratesys.edu", "SecretPass@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Staff account is inactive. Please contact the system administrator."));
    }

    @Test
    @DisplayName("6. Passwords must be stored as BCrypt hashes, not in plaintext")
    void testPasswordIsStoredHashedNotPlaintext() {
        Staff staff = staffRepository.findByEmail("tech.sarah@calibratesys.edu").orElseThrow();
        String hash = staff.getPasswordHash();

        assertNotNull(hash);
        assertNotEquals("SecretPass@123", hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("SecretPass@123", hash));
    }

    @Test
    @DisplayName("7. Protected endpoint /api/auth/me returns 401 when unauthenticated")
    void testProtectedEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("8. Protected endpoint /api/auth/me returns 200 with valid Bearer token")
    void testProtectedEndpointWithValidToken() throws Exception {
        String token = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(activeLabStaff));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffId").value("TECH-001"))
                .andExpect(jsonPath("$.email").value("tech.sarah@calibratesys.edu"))
                .andExpect(jsonPath("$.systemRole").value("LAB_STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("9. Protected endpoint with malformed/invalid token returns 401")
    void testProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer malformed.token.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("10. Role-based restriction: LAB_STAFF receives 403 Forbidden for ADMIN-only endpoint")
    void testRoleBasedRestrictionForStaff() throws Exception {
        String staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(activeLabStaff));

        mockMvc.perform(get("/api/auth/admin-check")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("11. Role-based access: ADMIN receives 200 OK for ADMIN-only endpoint")
    void testRoleBasedAccessForAdmin() throws Exception {
        String adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(adminStaff));

        mockMvc.perform(get("/api/auth/admin-check")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
