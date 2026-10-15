package com.calibratesys.controller;

import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDemoDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        Staff labStaff = staffRepository.findByEmail("arun.kumar@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-001", "Arun", "Kumar", "arun.kumar@calibratesys.edu",
                        passwordEncoder.encode("Staff@12345"), "+91-9876543211",
                        "Laboratory Incharge", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(labStaff));
    }

    @Test
    @DisplayName("LAB_STAFF role is forbidden from triggering demo data reset")
    void testStaffCannotResetDemoData() throws Exception {
        mockMvc.perform(post("/api/admin/demo-data/reset")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request is rejected for demo data reset")
    void testUnauthenticatedCannotResetDemoData() throws Exception {
        mockMvc.perform(post("/api/admin/demo-data/reset")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN role can retrieve demo entity counts")
    void testAdminCanGetCounts() throws Exception {
        mockMvc.perform(get("/api/admin/demo-data/counts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.counts").isMap());
    }

    @Test
    @DisplayName("ADMIN role can reset demo dataset and verify exact entity counts")
    void testAdminCanResetDemoData() throws Exception {
        mockMvc.perform(post("/api/admin/demo-data/reset")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.counts.colleges").value(1))
                .andExpect(jsonPath("$.counts.laboratories").value(2))
                .andExpect(jsonPath("$.counts.staff").value(5))
                .andExpect(jsonPath("$.counts.laboratory_staff").value(5))
                .andExpect(jsonPath("$.counts.equipment").value(6))
                .andExpect(jsonPath("$.counts.calibration_schedules").value(6))
                .andExpect(jsonPath("$.counts.calibration_records").value(4))
                .andExpect(jsonPath("$.counts.calibration_documents").value(3))
                .andExpect(jsonPath("$.counts.notifications").value(5))
                .andExpect(jsonPath("$.counts.audit_logs").value(11));
    }
}

