package com.calibratesys.controller;

import com.calibratesys.dto.EquipmentRequestDto;
import com.calibratesys.entity.CalibrationRecord;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.CalibrationResult;
import com.calibratesys.entity.enums.EquipmentStatus;
import com.calibratesys.entity.enums.LabStatus;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.CalibrationRecordRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EquipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private CalibrationRecordRepository calibrationRecordRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.calibratesys.repository.LaboratoryStaffRepository laboratoryStaffRepository;

    private String adminToken;
    private String staffToken;
    private Laboratory defaultLab;

    @BeforeEach
    void setUp() {
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        Staff staff = staffRepository.findByEmail("equip.staff@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-EQ-01", "Emma", "Watson", "equip.staff@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0177",
                        "Lab Assistant", "Chemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff));

        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-EQ-" + unique, "Equipment College " + unique, null, null, null));
        defaultLab = laboratoryRepository.save(new Laboratory(col, "L-EQ-" + unique, "Default Lab " + unique, "Chemistry", "Room 101", LabStatus.ACTIVE));
        laboratoryStaffRepository.save(new com.calibratesys.entity.LaboratoryStaff(defaultLab, staff, com.calibratesys.entity.enums.StaffLabRole.LAB_TECHNICIAN, false));
    }


    @Test
    @DisplayName("1. ADMIN can create equipment successfully (201 Created)")
    void testAdminCanCreateEquipment() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        EquipmentRequestDto request = new EquipmentRequestDto(
                defaultLab.getId(), "AST-" + unique, "SN-" + unique,
                "Analytical Balance", "AB-204", "Mettler Toledo",
                LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 20),
                90, "OPERATIONAL"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.assetTag").value("AST-" + unique))
                .andExpect(jsonPath("$.name").value("Analytical Balance"))
                .andExpect(jsonPath("$.laboratoryId").value(defaultLab.getId()))
                .andExpect(jsonPath("$.calibrationFrequencyDays").value(90))
                .andExpect(jsonPath("$.status").value("OPERATIONAL"));
    }

    @Test
    @DisplayName("2. Equipment creation requires existing laboratory (404 Not Found)")
    void testEquipmentCreationRequiresExistingLaboratory() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        EquipmentRequestDto request = new EquipmentRequestDto(
                999999L, "AST-NOLAB-" + unique, "SN-NOLAB-" + unique,
                "Spectrophotometer", "SP-100", "Shimadzu",
                null, null, 180, "OPERATIONAL"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Laboratory not found")));
    }

    @Test
    @DisplayName("3. Required fields validation (missing asset tag, missing name, missing frequency) -> 400 Bad Request")
    void testRequiredFieldsValidation() throws Exception {
        EquipmentRequestDto invalid = new EquipmentRequestDto(
                defaultLab.getId(), "", null, "", null, null, null, null, null, "OPERATIONAL"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Invalid equipment status is rejected (400 Bad Request)")
    void testInvalidStatusRejected() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        EquipmentRequestDto request = new EquipmentRequestDto(
                defaultLab.getId(), "AST-STAT-" + unique, null,
                "Oscilloscope", "OSC-1", "Tektronix",
                null, null, 365, "BROKEN_INVALID_STATUS"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid equipment status")));
    }

    @Test
    @DisplayName("5. Non-positive calibration frequency is rejected (400 Bad Request)")
    void testNonPositiveCalibrationFrequencyRejected() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        EquipmentRequestDto request = new EquipmentRequestDto(
                defaultLab.getId(), "AST-FREQ-" + unique, null,
                "Centrifuge", "CF-50", "Eppendorf",
                null, null, -10, "OPERATIONAL"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. Duplicate asset tag is rejected (409 Conflict)")
    void testDuplicateAssetTagRejected() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        equipmentRepository.save(new Equipment(
                defaultLab, "AST-DUP-" + unique, "SN-DUP1-" + unique,
                "Device 1", "M-1", "Mfr", null, null, 90, EquipmentStatus.OPERATIONAL
        ));

        EquipmentRequestDto duplicate = new EquipmentRequestDto(
                defaultLab.getId(), "AST-DUP-" + unique, "SN-DUP2-" + unique,
                "Device 2", "M-2", "Mfr", null, null, 90, "OPERATIONAL"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @DisplayName("7. Duplicate serial number is rejected (409 Conflict)")
    void testDuplicateSerialNumberRejected() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        equipmentRepository.save(new Equipment(
                defaultLab, "AST-SNDUP1-" + unique, "SN-MATCH-" + unique,
                "Device A", "M-A", "Mfr", null, null, 90, EquipmentStatus.OPERATIONAL
        ));

        EquipmentRequestDto duplicate = new EquipmentRequestDto(
                defaultLab.getId(), "AST-SNDUP2-" + unique, "SN-MATCH-" + unique,
                "Device B", "M-B", "Mfr", null, null, 90, "OPERATIONAL"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("serial number")));
    }

    @Test
    @DisplayName("8. Serial number is optional (null or empty serial number allowed)")
    void testOptionalSerialNumberAllowed() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        EquipmentRequestDto request = new EquipmentRequestDto(
                defaultLab.getId(), "AST-NOSN-" + unique, null,
                "Glassware Oven", "GO-10", "Fisher",
                null, null, 180, "OPERATIONAL"
        );

        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serialNumber").doesNotExist());
    }

    @Test
    @DisplayName("9. ADMIN and LAB_STAFF can retrieve equipment by ID (200 OK)")
    void testGetEquipmentById() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        Equipment saved = equipmentRepository.save(new Equipment(
                defaultLab, "AST-GET-" + unique, "SN-GET-" + unique,
                "pH Meter", "PH-200", "Oakton", null, null, 30, EquipmentStatus.OPERATIONAL
        ));

        // ADMIN retrieve
        mockMvc.perform(get("/api/equipment/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.assetTag").value("AST-GET-" + unique));

        // LAB_STAFF retrieve
        mockMvc.perform(get("/api/equipment/" + saved.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()));
    }

    @Test
    @DisplayName("10. Retrieve equipment by laboratory (GET /api/laboratories/{id}/equipment)")
    void testGetEquipmentByLaboratory() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        equipmentRepository.save(new Equipment(
                defaultLab, "AST-LBEQ-" + unique, null,
                "Autoclave", "AC-1", "Tuttnauer", null, null, 60, EquipmentStatus.OPERATIONAL
        ));

        mockMvc.perform(get("/api/laboratories/" + defaultLab.getId() + "/equipment")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].laboratoryId").value(defaultLab.getId()));
    }

    @Test
    @DisplayName("11. Filter equipment by laboratory and status (200 OK)")
    void testFilterEquipmentByLaboratoryAndStatus() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        equipmentRepository.save(new Equipment(
                defaultLab, "AST-FLT1-" + unique, null,
                "Microscope", "MC-10", "Olympus", null, null, 180, EquipmentStatus.UNDER_MAINTENANCE
        ));

        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("laboratoryId", String.valueOf(defaultLab.getId()))
                        .param("status", "UNDER_MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("12. Search equipment by keyword across name, asset tag, or serial number (200 OK)")
    void testSearchEquipment() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        equipmentRepository.save(new Equipment(
                defaultLab, "AST-SRCH-" + unique, "SN-SRCH-" + unique,
                "Thermal Cycler PCR", "TC-96", "Bio-Rad", null, null, 90, EquipmentStatus.OPERATIONAL
        ));

        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("search", "Cycler PCR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Thermal Cycler PCR"));
    }

    @Test
    @DisplayName("13. ADMIN can update equipment details and reassign laboratory (200 OK)")
    void testAdminCanUpdateEquipment() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        Equipment saved = equipmentRepository.save(new Equipment(
                defaultLab, "AST-UPD-" + unique, "SN-UPD-" + unique,
                "Old Name", "M-OLD", "Old Mfr", null, null, 30, EquipmentStatus.OPERATIONAL
        ));

        // Create target laboratory for reassignment
        Laboratory newLab = laboratoryRepository.save(new Laboratory(
                defaultLab.getCollege(), "L-NEW-" + unique, "New Target Lab", "Biology", "Room B2", LabStatus.ACTIVE
        ));

        EquipmentRequestDto updateDto = new EquipmentRequestDto(
                newLab.getId(), "AST-UPD-" + unique, "SN-UPD-" + unique,
                "Updated Incubator", "INC-500", "Thermo Fisher",
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 10),
                60, "CALIBRATION_DUE"
        );

        mockMvc.perform(put("/api/equipment/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Incubator"))
                .andExpect(jsonPath("$.laboratoryId").value(newLab.getId()))
                .andExpect(jsonPath("$.status").value("CALIBRATION_DUE"))
                .andExpect(jsonPath("$.calibrationFrequencyDays").value(60));
    }

    @Test
    @DisplayName("14. ADMIN can delete equipment when safe (204 No Content)")
    void testAdminCanDeleteEquipmentWhenSafe() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        Equipment saved = equipmentRepository.save(new Equipment(
                defaultLab, "AST-DEL-" + unique, null,
                "Temporary Equipment", "T-1", "Mfr", null, null, 365, EquipmentStatus.DECOMMISSIONED
        ));

        mockMvc.perform(delete("/api/equipment/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(equipmentRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("15. Equipment deletion is blocked if historical calibration records exist (409 Conflict)")
    void testDeleteBlockedWhenCalibrationRecordsExist() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        Equipment equipment = equipmentRepository.save(new Equipment(
                defaultLab, "AST-SAFE-" + unique, null,
                "Critical Centrifuge", "CC-1", "Beckman", null, null, 90, EquipmentStatus.OPERATIONAL
        ));

        Staff staff = staffRepository.findByEmail("admin@calibratesys.edu").orElseThrow();

        // Create a calibration record referencing this equipment
        CalibrationRecord record = new CalibrationRecord();
        record.setEquipment(equipment);
        record.setCalibrationDate(LocalDate.now().minusDays(10));
        record.setNextDueDate(LocalDate.now().plusDays(80));
        record.setCalibratedByType(com.calibratesys.entity.enums.CalibratedByType.INTERNAL);
        record.setCalibratorName("Certified Calibrator");
        record.setResult(CalibrationResult.PASSED);
        record.setNotes("Calibrated by certified technician");
        calibrationRecordRepository.save(record);

        mockMvc.perform(delete("/api/equipment/" + equipment.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("calibration records")));
    }

    @Test
    @DisplayName("16. LAB_STAFF cannot perform write operations (403 Forbidden)")
    void testLabStaffWriteOperationsForbidden() throws Exception {
        EquipmentRequestDto request = new EquipmentRequestDto(
                defaultLab.getId(), "AST-FORBID", null, "Forbidden Equipment",
                null, null, null, null, 90, "OPERATIONAL"
        );

        // POST -> 403
        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // PUT -> 403
        mockMvc.perform(put("/api/equipment/1")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // DELETE -> 403
        mockMvc.perform(delete("/api/equipment/1")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("17. Unauthenticated requests to /api/equipment return 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isUnauthorized());
    }
}
