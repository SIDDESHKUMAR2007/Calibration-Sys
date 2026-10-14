package com.calibratesys.controller;

import com.calibratesys.dto.LaboratoryStaffRequestDto;
import com.calibratesys.dto.LaboratoryStaffUpdateDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LaboratoryStaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private LaboratoryStaffRepository laboratoryStaffRepository;

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
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        Staff staff = staffRepository.findByEmail("assign.tester@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-ASG-01", "Assignment", "Tester", "assign.tester@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0188",
                        "Lab Assistant", "Chemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff));
    }

    @Test
    @DisplayName("1. ADMIN can assign staff to laboratory (201 Created)")
    void testAdminCanAssignStaffToLaboratory() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-ASG1-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-ASG1-" + unique, "Lab 1", "Physics", "Room 1", LabStatus.ACTIVE));
        Staff staff = staffRepository.save(new Staff(
                "STF-A1-" + unique, "Alpha", "One", "alpha1." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        LaboratoryStaffRequestDto request = new LaboratoryStaffRequestDto(
                staff.getId(), "PRIMARY_INCHARGE", true, "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.laboratoryId").value(lab.getId()))
                .andExpect(jsonPath("$.staffId").value(staff.getId()))
                .andExpect(jsonPath("$.roleInLab").value("PRIMARY_INCHARGE"))
                .andExpect(jsonPath("$.primaryContact").value(true))
                .andExpect(jsonPath("$.assignmentStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("2. Assignment requires existing staff (404 Not Found)")
    void testAssignmentRequiresExistingStaff() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-ASG2-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-ASG2-" + unique, "Lab 2", "Physics", "Room 2", LabStatus.ACTIVE));

        LaboratoryStaffRequestDto request = new LaboratoryStaffRequestDto(
                999999L, "LAB_TECHNICIAN", false, "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. Assignment requires existing laboratory (404 Not Found)")
    void testAssignmentRequiresExistingLaboratory() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        Staff staff = staffRepository.save(new Staff(
                "STF-A3-" + unique, "Staff", "Three", "stf3." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        LaboratoryStaffRequestDto request = new LaboratoryStaffRequestDto(
                staff.getId(), "LAB_TECHNICIAN", false, "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories/999999/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. Duplicate staff/laboratory assignment is rejected (409 Conflict)")
    void testDuplicateAssignmentRejected() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-ASG4-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-ASG4-" + unique, "Lab 4", "Physics", "Room 4", LabStatus.ACTIVE));
        Staff staff = staffRepository.save(new Staff(
                "STF-A4-" + unique, "Staff", "Four", "stf4." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        // Initial assignment
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, staff, StaffLabRole.LAB_TECHNICIAN, false));

        // Attempt duplicate POST
        LaboratoryStaffRequestDto duplicate = new LaboratoryStaffRequestDto(
                staff.getId(), "STAFF", false, "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already assigned")));
    }

    @Test
    @DisplayName("5. ADMIN can update assignment role, primary contact, and status (200 OK)")
    void testUpdateStaffAssignment() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-ASG5-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-ASG5-" + unique, "Lab 5", "Physics", "Room 5", LabStatus.ACTIVE));
        Staff staff = staffRepository.save(new Staff(
                "STF-A5-" + unique, "Staff", "Five", "stf5." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        laboratoryStaffRepository.save(new LaboratoryStaff(lab, staff, StaffLabRole.STAFF, false));

        LaboratoryStaffUpdateDto updateDto = new LaboratoryStaffUpdateDto(
                "SAFETY_OFFICER", true, "ACTIVE"
        );

        mockMvc.perform(put("/api/laboratories/" + lab.getId() + "/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleInLab").value("SAFETY_OFFICER"))
                .andExpect(jsonPath("$.primaryContact").value(true));
    }

    @Test
    @DisplayName("6. Many-to-Many: Staff can belong to multiple laboratories")
    void testStaffAssignedToMultipleLaboratories() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-M2M1-" + unique, "College " + unique, null, null, null));
        Laboratory lab1 = laboratoryRepository.save(new Laboratory(col, "L-M2M-A" + unique, "Lab A", "Physics", "Room A", LabStatus.ACTIVE));
        Laboratory lab2 = laboratoryRepository.save(new Laboratory(col, "L-M2M-B" + unique, "Lab B", "Physics", "Room B", LabStatus.ACTIVE));

        Staff staff = staffRepository.save(new Staff(
                "STF-M2M-" + unique, "Multi", "LabStaff", "multilab." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Coordinator", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        laboratoryStaffRepository.save(new LaboratoryStaff(lab1, staff, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(lab2, staff, StaffLabRole.CALIBRATION_COORDINATOR, false));

        mockMvc.perform(get("/api/staff/" + staff.getId() + "/laboratories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("7. Many-to-Many: Laboratory can have multiple staff members")
    void testLaboratoryHasMultipleStaff() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-M2M2-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-SHR-" + unique, "Shared Lab", "Physics", "Room S", LabStatus.ACTIVE));

        Staff staff1 = staffRepository.save(new Staff(
                "STF-SH1-" + unique, "Tech", "One", "sh1." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        Staff staff2 = staffRepository.save(new Staff(
                "STF-SH2-" + unique, "Tech", "Two", "sh2." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        laboratoryStaffRepository.save(new LaboratoryStaff(lab, staff1, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, staff2, StaffLabRole.LAB_TECHNICIAN, false));

        mockMvc.perform(get("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("8. Primary contact rule: Laboratory has at most 1 active primary contact; assigning a new primary contact demotes the previous")
    void testPrimaryContactDemotionHandover() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-PC-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-PC-" + unique, "PC Lab", "Physics", "Room PC", LabStatus.ACTIVE));

        Staff previousPrimary = staffRepository.save(new Staff(
                "STF-PREV-" + unique, "Old", "Primary", "prev." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        Staff newPrimary = staffRepository.save(new Staff(
                "STF-NEW-" + unique, "New", "Primary", "new." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        // Assign previousPrimary as primary contact
        LaboratoryStaff prevAssignment = new LaboratoryStaff(lab, previousPrimary, StaffLabRole.PRIMARY_INCHARGE, true);
        prevAssignment.setStatus("ACTIVE");
        laboratoryStaffRepository.save(prevAssignment);

        // Assign newPrimary as primary contact via POST
        LaboratoryStaffRequestDto newRequest = new LaboratoryStaffRequestDto(
                newPrimary.getId(), "PRIMARY_INCHARGE", true, "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primaryContact").value(true));

        // Verify previousPrimary was automatically demoted
        LaboratoryStaff reloadedPrev = laboratoryStaffRepository.findByLaboratoryIdAndStaffId(lab.getId(), previousPrimary.getId()).orElseThrow();
        assertFalse(reloadedPrev.isPrimaryContact(), "Previous primary contact should have been automatically demoted");

        // Verify only 1 active primary contact exists in this laboratory
        var activePrimaryList = laboratoryStaffRepository.findByLaboratoryIdAndIsPrimaryContactTrue(lab.getId());
        assertEquals(1, activePrimaryList.size());
        assertEquals(newPrimary.getId(), activePrimaryList.get(0).getStaff().getId());
    }

    @Test
    @DisplayName("9. Inactive staff cannot be given an ACTIVE assignment or made primary contact (400 Bad Request)")
    void testInactiveStaffCannotHaveActiveAssignmentOrPrimaryContact() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-INACT-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-INACT-" + unique, "Inact Lab", "Physics", "Room I", LabStatus.ACTIVE));

        Staff inactiveStaff = staffRepository.save(new Staff(
                "STF-INACT-" + unique, "Inactive", "Staff", "inact." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.INACTIVE
        ));

        // Attempt 1: Active assignment for inactive staff -> 400
        LaboratoryStaffRequestDto req1 = new LaboratoryStaffRequestDto(
                inactiveStaff.getId(), "LAB_TECHNICIAN", false, "ACTIVE"
        );
        mockMvc.perform(post("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Inactive staff member cannot be given an ACTIVE")));

        // Attempt 2: Primary contact for inactive staff -> 400
        LaboratoryStaffRequestDto req2 = new LaboratoryStaffRequestDto(
                inactiveStaff.getId(), "PRIMARY_INCHARGE", true, "INACTIVE"
        );
        mockMvc.perform(post("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Inactive staff member cannot be assigned as primary contact")));
    }

    @Test
    @DisplayName("10. Inactive assignment cannot be designated as primary contact (400 Bad Request)")
    void testInactiveAssignmentCannotBePrimaryContact() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-INASG-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-INASG-" + unique, "Inasg Lab", "Physics", "Room IA", LabStatus.ACTIVE));

        Staff activeStaff = staffRepository.save(new Staff(
                "STF-INASG-" + unique, "Active", "Staff", "inasg." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        LaboratoryStaffRequestDto req = new LaboratoryStaffRequestDto(
                activeStaff.getId(), "STAFF", true, "INACTIVE"
        );

        mockMvc.perform(post("/api/laboratories/" + lab.getId() + "/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("inactive assignment cannot be designated as primary contact")));
    }

    @Test
    @DisplayName("11. ADMIN can remove assignment (204 No Content)")
    void testAdminCanRemoveAssignment() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-RM-" + unique, "College " + unique, null, null, null));
        Laboratory lab = laboratoryRepository.save(new Laboratory(col, "L-RM-" + unique, "RM Lab", "Physics", "Room RM", LabStatus.ACTIVE));

        Staff staff = staffRepository.save(new Staff(
                "STF-RM-" + unique, "Remove", "Staff", "rm." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Pass@123"), null, "Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        laboratoryStaffRepository.save(new LaboratoryStaff(lab, staff, StaffLabRole.STAFF, false));

        mockMvc.perform(delete("/api/laboratories/" + lab.getId() + "/staff/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(laboratoryStaffRepository.existsByLaboratoryIdAndStaffId(lab.getId(), staff.getId()));
    }

    @Test
    @DisplayName("12. Unauthenticated requests to assignment endpoints return 401 Unauthorized")
    void testUnauthenticatedAssignmentReturns401() throws Exception {
        mockMvc.perform(get("/api/laboratories/1/staff"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("13. Regular LAB_STAFF cannot perform assignment writes (403 Forbidden)")
    void testLabStaffAssignmentWriteReturns403() throws Exception {
        LaboratoryStaffRequestDto req = new LaboratoryStaffRequestDto(1L, "STAFF", false, "ACTIVE");

        mockMvc.perform(post("/api/laboratories/1/staff")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
