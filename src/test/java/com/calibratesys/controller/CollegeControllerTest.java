package com.calibratesys.controller;

import com.calibratesys.dto.CollegeRequestDto;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.LabStatus;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.CollegeRepository;
import com.calibratesys.repository.LaboratoryRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CollegeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private StaffRepository staffRepository;

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
        // Ensure Admin user
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        // Ensure Staff user
        Staff staff = staffRepository.findByEmail("college.test.staff@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-COL-01", "Mark", "Miller", "college.test.staff@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0188",
                        "Lab Assistant", "Chemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff));
    }

    @Test
    @DisplayName("1. ADMIN can create a college successfully (201 Created)")
    void testAdminCanCreateCollege() throws Exception {
        String uniqueCode = "COL-CR-" + System.currentTimeMillis();
        CollegeRequestDto request = new CollegeRequestDto(
                uniqueCode, "Faculty of Science & Tech", "North Campus", "contact@sci.edu", "+1-555-0144"
        );

        mockMvc.perform(post("/api/colleges")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value(uniqueCode))
                .andExpect(jsonPath("$.name").value("Faculty of Science & Tech"))
                .andExpect(jsonPath("$.contactEmail").value("contact@sci.edu"));
    }

    @Test
    @DisplayName("2. ADMIN can retrieve a college by ID (200 OK)")
    void testAdminCanRetrieveCollege() throws Exception {
        College college = collegeRepository.save(new College(
                "COL-GET-" + System.currentTimeMillis(), "Arts and Sciences", "East Wing", "art@sci.edu", "555-0122"
        ));

        mockMvc.perform(get("/api/colleges/" + college.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(college.getId()))
                .andExpect(jsonPath("$.code").value(college.getCode()));
    }

    @Test
    @DisplayName("3. LAB_STAFF can read/list colleges (200 OK)")
    void testStaffCanListColleges() throws Exception {
        mockMvc.perform(get("/api/colleges")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("4. ADMIN can update an existing college (200 OK)")
    void testAdminCanUpdateCollege() throws Exception {
        College college = collegeRepository.save(new College(
                "COL-UPD-" + System.currentTimeMillis(), "Original College Name", "Main St", "orig@col.edu", "555-0100"
        ));

        CollegeRequestDto updateRequest = new CollegeRequestDto(
                college.getCode(), "Updated College Name", "Main St Updated", "updated@col.edu", "+1-555-0199"
        );

        mockMvc.perform(put("/api/colleges/" + college.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated College Name"))
                .andExpect(jsonPath("$.contactEmail").value("updated@col.edu"));
    }

    @Test
    @DisplayName("5. ADMIN can delete a college when safe (204 No Content)")
    void testAdminCanDeleteCollegeWhenSafe() throws Exception {
        College college = collegeRepository.save(new College(
                "COL-DEL-" + System.currentTimeMillis(), "Empty College", "No Labs Campus", null, null
        ));

        mockMvc.perform(delete("/api/colleges/" + college.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(collegeRepository.findById(college.getId()).isPresent());
    }

    @Test
    @DisplayName("6. Duplicate college code is rejected with 409 Conflict")
    void testDuplicateCollegeCodeRejected() throws Exception {
        String duplicateCode = "COL-DUP-" + System.currentTimeMillis();
        collegeRepository.save(new College(duplicateCode, "First Instance", "Addr", null, null));

        CollegeRequestDto duplicateRequest = new CollegeRequestDto(
                duplicateCode, "Second Instance", "Addr 2", null, null
        );

        mockMvc.perform(post("/api/colleges")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("College with code '" + duplicateCode + "' already exists"));
    }

    @Test
    @DisplayName("7. Invalid college data (blank code/name, bad email) is rejected with 400 Bad Request")
    void testInvalidCollegeDataRejected() throws Exception {
        // Blank code and invalid email
        CollegeRequestDto invalidRequest = new CollegeRequestDto(
                "", "", "Some Address", "invalid-email-address", "not-a-phone"
        );

        mockMvc.perform(post("/api/colleges")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("8. Delete college is blocked with 409 Conflict when dependent laboratories exist")
    void testDeleteCollegeBlockedWhenLaboratoriesExist() throws Exception {
        College college = collegeRepository.save(new College(
                "COL-BLOCKED-" + System.currentTimeMillis(), "College with Labs", "Tech Park", null, null
        ));

        laboratoryRepository.save(new Laboratory(
                college, "LAB-SUB-" + System.currentTimeMillis(), "Physics Lab", "Physics", "Room 101", LabStatus.ACTIVE
        ));

        mockMvc.perform(delete("/api/colleges/" + college.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete college because laboratories are associated with it. Delete or reassign laboratories first."));
    }

    @Test
    @DisplayName("9. Unauthenticated write request returns 401 Unauthorized")
    void testUnauthenticatedWriteRejected() throws Exception {
        CollegeRequestDto request = new CollegeRequestDto("COL-UNAUTH", "Unauth College", null, null, null);

        mockMvc.perform(post("/api/colleges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("10. LAB_STAFF write request is rejected with 403 Forbidden")
    void testLabStaffWriteForbidden() throws Exception {
        CollegeRequestDto request = new CollegeRequestDto("COL-FORBIDDEN", "Forbidden College", null, null, null);

        mockMvc.perform(post("/api/colleges")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("11. Non-existent college returns 404 Not Found")
    void testCollegeNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/colleges/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("College not found with id: 999999"));
    }
}
