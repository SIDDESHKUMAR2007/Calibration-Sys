package com.calibratesys.controller;

import com.calibratesys.dto.StaffRequestDto;
import com.calibratesys.dto.StaffUpdateRequestDto;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.LaboratoryStaff;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.LabStatus;
import com.calibratesys.entity.enums.StaffLabRole;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.CollegeRepository;
import com.calibratesys.repository.LaboratoryRepository;
import com.calibratesys.repository.LaboratoryStaffRepository;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.security.JwtTokenProvider;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LaboratoryStaffRepository laboratoryStaffRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        // Provision Admin
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        // Provision Regular Lab Staff
        Staff labStaff = staffRepository.findByEmail("staff.tester@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-TEST-01", "Alice", "Brown", "staff.tester@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0188",
                        "Lab Assistant", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(labStaff));
    }

    @Test
    @DisplayName("1. ADMIN can create staff successfully (201 Created)")
    void testAdminCanCreateStaff() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        StaffRequestDto request = new StaffRequestDto(
                "STF-N-" + unique, "John", "Doe", "john.doe." + unique + "@calibratesys.edu",
                "+1-555-2233", "Senior Technician", "Mechanical", "LAB_STAFF", "ACTIVE", "Password@123"
        );

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.staffId").value("STF-N-" + unique))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe." + unique + "@calibratesys.edu"))
                .andExpect(jsonPath("$.systemRole").value("LAB_STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("2. Staff response never exposes password or passwordHash")
    void testPasswordHashIsNotExposed() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        StaffRequestDto request = new StaffRequestDto(
                "STF-SEC-" + unique, "Security", "Tester", "sec." + unique + "@calibratesys.edu",
                null, "Analyst", "Cybersecurity", "LAB_STAFF", "ACTIVE", "SuperSecretPass99"
        );

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("3. ADMIN can retrieve staff by ID (200 OK)")
    void testGetStaffById() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        Staff staff = staffRepository.save(new Staff(
                "STF-GET-" + unique, "Robert", "Frost", "rfrost." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), "555-4455", "Lecturer", "English",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        mockMvc.perform(get("/api/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(staff.getId()))
                .andExpect(jsonPath("$.staffId").value("STF-GET-" + unique))
                .andExpect(jsonPath("$.email").value(staff.getEmail()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("4. Authenticated users can list staff and filter by status and department")
    void testListStaffWithFiltering() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        staffRepository.save(new Staff(
                "STF-FLT-" + unique, "Filter", "User", "filter." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Specialist", "Biomedical",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        mockMvc.perform(get("/api/staff")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("department", "Biomedical")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("5. ADMIN can update staff information (200 OK)")
    void testUpdateStaff() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        Staff staff = staffRepository.save(new Staff(
                "STF-UPD-" + unique, "Before", "Update", "before." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), "555-1111", "Junior Tech", "Physics",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        StaffUpdateRequestDto updateDto = new StaffUpdateRequestDto(
                "STF-UPD-" + unique, "After", "Update", "after." + unique + "@calibratesys.edu",
                "+1-555-9999", "Senior Tech", "Physics", "LAB_STAFF", "ACTIVE", null
        );

        mockMvc.perform(put("/api/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("After"))
                .andExpect(jsonPath("$.email").value("after." + unique + "@calibratesys.edu"))
                .andExpect(jsonPath("$.designation").value("Senior Tech"));
    }

    @Test
    @DisplayName("6. Deactivating staff marks active laboratory assignments inactive and clears primary contact")
    void testDeactivatingStaffClearsPrimaryContactAndActiveAssignments() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        // Create college and laboratory
        College col = collegeRepository.save(new College("C-DEACT-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-DEACT-" + unique, "Lab " + unique, "Physics", "Room 1", LabStatus.ACTIVE));

        // Create active staff
        Staff staff = staffRepository.save(new Staff(
                "STF-DEACT-" + unique, "Deact", "Staff", "deact." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        // Assign staff as primary contact
        LaboratoryStaff assignment = new LaboratoryStaff(lab, staff, StaffLabRole.PRIMARY_INCHARGE, true);
        assignment.setStatus("ACTIVE");
        laboratoryStaffRepository.save(assignment);

        // Deactivate staff
        StaffUpdateRequestDto deactDto = new StaffUpdateRequestDto(
                staff.getStaffId(), staff.getFirstName(), staff.getLastName(), staff.getEmail(),
                staff.getPhoneNumber(), staff.getDesignation(), staff.getDepartment(),
                staff.getSystemRole().name(), "INACTIVE", null
        );

        mockMvc.perform(put("/api/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // Verify assignment is marked INACTIVE and primary contact is false
        LaboratoryStaff updatedAssignment = laboratoryStaffRepository.findByLaboratoryIdAndStaffId(lab.getId(), staff.getId()).orElseThrow();
        assertFalse(updatedAssignment.isPrimaryContact());
        org.junit.jupiter.api.Assertions.assertEquals("INACTIVE", updatedAssignment.getStatus());
    }

    @Test
    @DisplayName("7. Duplicate staff ID is rejected (409 Conflict)")
    void testDuplicateStaffIdRejected() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        staffRepository.save(new Staff(
                "STF-DUP-" + unique, "Original", "User", "orig." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        StaffRequestDto duplicate = new StaffRequestDto(
                "STF-DUP-" + unique, "Another", "User", "diff." + unique + "@calibratesys.edu",
                null, "Tech", "Physics", "LAB_STAFF", "ACTIVE", "Password@123"
        );

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already in use")));
    }

    @Test
    @DisplayName("8. Duplicate email is rejected (409 Conflict)")
    void testDuplicateEmailRejected() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        staffRepository.save(new Staff(
                "STF-EM1-" + unique, "Email", "One", "dup.email." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        StaffRequestDto duplicate = new StaffRequestDto(
                "STF-EM2-" + unique, "Email", "Two", "dup.email." + unique + "@calibratesys.edu",
                null, "Tech", "Physics", "LAB_STAFF", "ACTIVE", "Password@123"
        );

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already registered")));
    }

    @Test
    @DisplayName("9. Invalid staff data is rejected (400 Bad Request)")
    void testInvalidStaffDataRejected() throws Exception {
        StaffRequestDto invalid = new StaffRequestDto(
                "", "", "", "invalid-email-format",
                "invalid phone format @@!", "", "", "INVALID_ROLE", "INVALID_STATUS", "short"
        );

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("10. Unauthenticated requests to /api/staff return 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/staff"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. Regular LAB_STAFF cannot create or modify staff (403 Forbidden)")
    void testLabStaffWriteReturns403() throws Exception {
        StaffRequestDto request = new StaffRequestDto(
                "STF-FORBID", "Jane", "Staff", "forbidden@calibratesys.edu",
                null, "Tech", "Chemistry", "LAB_STAFF", "ACTIVE", "Password@123"
        );

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Staff with existing laboratory assignments cannot be deleted (409 Conflict)")
    void testStaffWithAssignmentsCannotBeDeleted() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        College col = collegeRepository.save(new College("C-DEL-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-DEL-" + unique, "Lab " + unique, "Physics", "Room 1", LabStatus.ACTIVE));

        Staff staff = staffRepository.save(new Staff(
                "STF-DEL1-" + unique, "Assigned", "Staff", "del1." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        LaboratoryStaff assignment = new LaboratoryStaff(lab, staff, StaffLabRole.STAFF, false);
        laboratoryStaffRepository.save(assignment);

        mockMvc.perform(delete("/api/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("existing laboratory assignments")));
    }

    @Test
    @DisplayName("13. Staff without assignments can be deleted (204 No Content)")
    void testStaffWithoutAssignmentsCanBeDeleted() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        Staff staff = staffRepository.save(new Staff(
                "STF-DEL2-" + unique, "Free", "Staff", "del2." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        mockMvc.perform(delete("/api/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(staffRepository.existsById(staff.getId()));
    }
}
