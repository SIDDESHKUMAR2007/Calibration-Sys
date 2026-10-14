package com.calibratesys.controller;

import com.calibratesys.dto.LaboratoryRequestDto;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.EquipmentStatus;
import com.calibratesys.entity.enums.LabStatus;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.CollegeRepository;
import com.calibratesys.repository.EquipmentRepository;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LaboratoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

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
    private College testCollege;

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
        Staff staff = staffRepository.findByEmail("lab.test.staff@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-LAB-01", "Emma", "Watson", "lab.test.staff@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0177",
                        "Lab Technician", "Biotech", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff));

        // Create or get base test college
        testCollege = collegeRepository.findByCode("COL-LAB-BASE")
                .orElseGet(() -> collegeRepository.save(new College(
                        "COL-LAB-BASE", "Base Test College", "Campus Base", "base@col.edu", "555-0100"
                )));
    }

    @Test
    @DisplayName("1. ADMIN can create a laboratory under an existing college (201 Created)")
    void testAdminCanCreateLaboratory() throws Exception {
        String uniqueLabCode = "LAB-CR-" + System.currentTimeMillis();
        LaboratoryRequestDto request = new LaboratoryRequestDto(
                testCollege.getId(), uniqueLabCode, "Robotics & Automation Lab", "Mechatronics", "Building C, Room 105", "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.labCode").value(uniqueLabCode))
                .andExpect(jsonPath("$.collegeId").value(testCollege.getId()))
                .andExpect(jsonPath("$.collegeCode").value("COL-LAB-BASE"))
                .andExpect(jsonPath("$.name").value("Robotics & Automation Lab"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("2. Laboratory creation fails with 404 when referenced college does not exist")
    void testLaboratoryMustReferenceExistingCollege() throws Exception {
        LaboratoryRequestDto request = new LaboratoryRequestDto(
                999999L, "LAB-GHOST-" + System.currentTimeMillis(), "Ghost Lab", "Physics", "Unknown", "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("College not found with id: 999999"));
    }

    @Test
    @DisplayName("3. ADMIN can retrieve a laboratory by ID (200 OK)")
    void testAdminCanRetrieveLaboratory() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(
                testCollege, "LAB-GET-" + System.currentTimeMillis(), "Optics Lab", "Physics", "Room 302", LabStatus.ACTIVE
        ));

        mockMvc.perform(get("/api/laboratories/" + lab.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lab.getId()))
                .andExpect(jsonPath("$.labCode").value(lab.getLabCode()));
    }

    @Test
    @DisplayName("4. LAB_STAFF can list laboratories (200 OK)")
    void testStaffCanListLaboratories() throws Exception {
        mockMvc.perform(get("/api/laboratories")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("5. Filtering laboratories by collegeId and status")
    void testFilteringLaboratories() throws Exception {
        // Query by collegeId
        mockMvc.perform(get("/api/laboratories?collegeId=" + testCollege.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Query by status
        mockMvc.perform(get("/api/laboratories?status=ACTIVE")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Query by both collegeId and status
        mockMvc.perform(get("/api/laboratories?collegeId=" + testCollege.getId() + "&status=ACTIVE")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("6. ADMIN can update an existing laboratory (200 OK)")
    void testAdminCanUpdateLaboratory() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(
                testCollege, "LAB-UPD-" + System.currentTimeMillis(), "Original Lab Name", "ECE", "Room 10", LabStatus.ACTIVE
        ));

        LaboratoryRequestDto updateRequest = new LaboratoryRequestDto(
                testCollege.getId(), lab.getLabCode(), "Updated Lab Name", "ECE", "Room 10 - Renovated", "UNDER_MAINTENANCE"
        );

        mockMvc.perform(put("/api/laboratories/" + lab.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Lab Name"))
                .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"));
    }

    @Test
    @DisplayName("7. ADMIN can delete a laboratory when safe (204 No Content)")
    void testAdminCanDeleteLaboratoryWhenSafe() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(
                testCollege, "LAB-DEL-" + System.currentTimeMillis(), "Empty Lab", "Civil", "Hangar 1", LabStatus.ACTIVE
        ));

        mockMvc.perform(delete("/api/laboratories/" + lab.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(laboratoryRepository.findById(lab.getId()).isPresent());
    }

    @Test
    @DisplayName("8. Duplicate laboratory code is rejected with 409 Conflict")
    void testDuplicateLaboratoryCodeRejected() throws Exception {
        String duplicateCode = "LAB-DUP-" + System.currentTimeMillis();
        laboratoryRepository.save(new Laboratory(
                testCollege, duplicateCode, "Primary Lab", "CS", "Lab 1", LabStatus.ACTIVE
        ));

        LaboratoryRequestDto duplicateRequest = new LaboratoryRequestDto(
                testCollege.getId(), duplicateCode, "Duplicate Attempt", "CS", "Lab 2", "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Laboratory with code '" + duplicateCode + "' already exists"));
    }

    @Test
    @DisplayName("9. Invalid laboratory data (bad status, blank fields) is rejected with 400 Bad Request")
    void testInvalidLaboratoryDataRejected() throws Exception {
        // Bad status
        LaboratoryRequestDto badStatus = new LaboratoryRequestDto(
                testCollege.getId(), "LAB-BAD-STAT", "Valid Name", "Physics", "Room 1", "INVALID_STATUS"
        );

        mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("10. Delete laboratory is blocked with 409 Conflict when equipment items are associated")
    void testDeleteLaboratoryBlockedWhenEquipmentExists() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(
                testCollege, "LAB-EQUIP-" + System.currentTimeMillis(), "Laser Lab", "Optics", "Bunker 2", LabStatus.ACTIVE
        ));

        equipmentRepository.save(new Equipment(
                lab, "ASSET-EQ-" + System.currentTimeMillis(), "SN-112233", "Laser Interferometer",
                "XL-80", "Renishaw", LocalDate.now(), LocalDate.now(), 180, EquipmentStatus.OPERATIONAL
        ));

        mockMvc.perform(delete("/api/laboratories/" + lab.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete laboratory because equipment items are registered under it. Reassign or remove equipment first."));
    }

    @Test
    @DisplayName("11. Unauthenticated write request returns 401 Unauthorized")
    void testUnauthenticatedWriteRejected() throws Exception {
        LaboratoryRequestDto request = new LaboratoryRequestDto(
                testCollege.getId(), "LAB-UNAUTH", "Unauth Lab", "Mech", null, "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("12. LAB_STAFF write request is rejected with 403 Forbidden")
    void testLabStaffWriteForbidden() throws Exception {
        LaboratoryRequestDto request = new LaboratoryRequestDto(
                testCollege.getId(), "LAB-FORBIDDEN", "Forbidden Lab", "Mech", null, "ACTIVE"
        );

        mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("13. Non-existent laboratory returns 404 Not Found")
    void testLaboratoryNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/laboratories/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Laboratory not found with id: 999999"));
    }
}
