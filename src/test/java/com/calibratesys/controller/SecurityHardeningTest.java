package com.calibratesys.controller;

import com.calibratesys.dto.LoginRequestDto;
import com.calibratesys.entity.*;
import com.calibratesys.entity.enums.*;
import com.calibratesys.repository.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LaboratoryStaffRepository laboratoryStaffRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private CalibrationScheduleRepository calibrationScheduleRepository;

    @Autowired
    private CalibrationRecordRepository calibrationRecordRepository;

    @Autowired
    private CalibrationDocumentRepository calibrationDocumentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String staffAToken;

    private Laboratory labA;
    private Laboratory labB;

    private Equipment eqA;
    private Equipment eqB;

    private CalibrationSchedule schedB;
    private CalibrationRecord recB;
    private CalibrationDocument docB;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        // 1. Create College
        College college = collegeRepository.save(new College("COL-SEC-" + unique, "Security College " + unique, null, null, null));

        // 2. Create Lab A and Lab B
        labA = laboratoryRepository.save(new Laboratory(college, "LAB-SEC-A-" + unique, "Sec Lab A", "Physics", "Room A", LabStatus.ACTIVE));
        labB = laboratoryRepository.save(new Laboratory(college, "LAB-SEC-B-" + unique, "Sec Lab B", "Chemistry", "Room B", LabStatus.ACTIVE));

        // 3. Create Admin Staff
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        // 4. Create Staff A (assigned ONLY to Lab A)
        Staff staffA = staffRepository.save(new Staff(
                "STF-SECA-" + unique, "Staff", "Alpha", "staff.alpha." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0181",
                "Lab Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        laboratoryStaffRepository.save(new LaboratoryStaff(labA, staffA, StaffLabRole.LAB_TECHNICIAN, false));
        staffAToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staffA));

        // 5. Equipment in Lab A
        eqA = equipmentRepository.save(new Equipment(
                labA, "TAG-SECA-" + unique, "SN-SECA-" + unique,
                "Microscope Alpha", "M-100", "Nikon",
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(25), 90, EquipmentStatus.OPERATIONAL
        ));

        // 6. Equipment, Schedule, Record, Document in Lab B (Unassigned to Staff A)
        eqB = equipmentRepository.save(new Equipment(
                labB, "TAG-SECB-" + unique, "SN-SECB-" + unique,
                "=cmd|' /C calc'!A0", "M-200", "Shimadzu",
                LocalDate.now().minusDays(60), LocalDate.now().minusDays(50), 60, EquipmentStatus.OPERATIONAL
        ));

        schedB = calibrationScheduleRepository.save(new CalibrationSchedule(
                eqB, LocalDate.now().minusDays(10), LocalDate.now().plusDays(20),
                ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Lab B schedule"
        ));

        CalibrationRecord rec = new CalibrationRecord();
        rec.setEquipment(eqB);
        rec.setCalibrationSchedule(schedB);
        rec.setCalibrationDate(LocalDate.now().minusDays(10));
        rec.setNextDueDate(LocalDate.now().plusDays(50));
        rec.setResult(CalibrationResult.PASSED);
        rec.setCalibratedByType(CalibratedByType.INTERNAL);
        rec.setCalibratorName("Certified Tech");
        rec.setNotes("All tests passed");
        recB = calibrationRecordRepository.save(rec);

        CalibrationDocument doc = new CalibrationDocument();
        doc.setEquipment(eqB);
        doc.setCalibrationRecord(recB);
        doc.setDocumentName("certificate-lab-b.pdf");
        doc.setFilePath("test-uuid-doc-b.pdf");
        doc.setFileType("application/pdf");
        doc.setFileSizeBytes(1024L);
        doc.setCertificateNumber("CERT-B-" + unique);
        doc.setIssueDate(LocalDate.now().minusDays(10));
        doc.setValidUntil(LocalDate.now().plusDays(50));
        docB = calibrationDocumentRepository.save(doc);

    }

    // =========================================================================
    // Multi-Lab Cross-Lab IDOR Tests
    // =========================================================================

    @Test
    @DisplayName("IDOR Protection: Staff A cannot access unassigned Lab B equipment by ID (403 Forbidden)")
    void testStaffCannotAccessUnassignedEquipmentById() throws Exception {
        // Staff A accessing Lab A equipment -> 200 OK
        mockMvc.perform(get("/api/equipment/" + eqA.getId())
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eqA.getId().intValue())));

        // Staff A accessing Lab B equipment -> 403 Forbidden
        mockMvc.perform(get("/api/equipment/" + eqB.getId())
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("IDOR Protection: Staff A cannot access unassigned Lab B calibration schedule by ID (403 Forbidden)")
    void testStaffCannotAccessUnassignedScheduleById() throws Exception {
        mockMvc.perform(get("/api/calibration-schedules/" + schedB.getId())
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("IDOR Protection: Staff A cannot access unassigned Lab B calibration record by ID (403 Forbidden)")
    void testStaffCannotAccessUnassignedRecordById() throws Exception {
        mockMvc.perform(get("/api/calibration-records/" + recB.getId())
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("IDOR Protection: Staff A cannot access unassigned Lab B calibration document by ID (403 Forbidden)")
    void testStaffCannotAccessUnassignedDocumentById() throws Exception {
        mockMvc.perform(get("/api/calibration-documents/" + docB.getId())
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("IDOR Protection: Staff A cannot download unassigned Lab B calibration document (403 Forbidden)")
    void testStaffCannotDownloadUnassignedDocument() throws Exception {
        mockMvc.perform(get("/api/calibration-documents/" + docB.getId() + "/download")
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("IDOR Protection: Staff A cannot view calibration schedules of unassigned Lab B equipment (403 Forbidden)")
    void testStaffCannotViewSchedulesOfUnassignedEquipment() throws Exception {
        mockMvc.perform(get("/api/equipment/" + eqB.getId() + "/calibration-schedules")
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("IDOR Protection: Staff A cannot view calibration records of unassigned Lab B equipment (403 Forbidden)")
    void testStaffCannotViewRecordsOfUnassignedEquipment() throws Exception {
        mockMvc.perform(get("/api/equipment/" + eqB.getId() + "/calibration-records")
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("IDOR Protection: Staff A cannot view staff assignments of unassigned Lab B (403 Forbidden)")
    void testStaffCannotViewStaffOfUnassignedLaboratory() throws Exception {
        mockMvc.perform(get("/api/laboratories/" + labB.getId() + "/staff")
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    // =========================================================================
    // Pagination Boundary Enforcement Tests
    // =========================================================================

    @Test
    @DisplayName("Pagination Limits: Negative page index returns 400 Bad Request")
    void testNegativePageIndexReturns400() throws Exception {
        mockMvc.perform(get("/api/equipment?page=-1&size=20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be less than zero")));
    }

    @Test
    @DisplayName("Pagination Limits: Zero or negative page size returns 400 Bad Request")
    void testZeroOrNegativePageSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/equipment?page=0&size=0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must not be less than one")));

        mockMvc.perform(get("/api/equipment?page=0&size=-5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must not be less than one")));
    }

    @Test
    @DisplayName("Pagination Limits: Excessively large page size returns 400 Bad Request (capped at 100)")
    void testExcessivePageSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/equipment?page=0&size=500")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size cannot exceed 100")));
    }

    // =========================================================================
    // CSV Injection / Formula Neutralization Tests
    // =========================================================================

    @Test
    @DisplayName("CSV Formula Injection: Malicious formulas starting with =, +, -, @ are sanitized with leading single quote")
    void testCsvFormulaInjectionSanitized() throws Exception {
        // Equipment B has name: =cmd|' /C calc'!A0
        var result = mockMvc.perform(get("/api/reports/equipment/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String csvContent = result.getResponse().getContentAsString();
        // The formula must be neutralized by prepending a single quote: '=cmd|' /C calc'!A0
        assertTrue(csvContent.contains("'=cmd|"), "Expected formula to be neutralized with leading single quote in CSV export");
    }

    // =========================================================================
    // Failed Login Auditing Tests
    // =========================================================================

    @Test
    @DisplayName("Failed Login Auditing: Failed login records LOGIN_FAILURE without leaking attempted password")
    void testFailedLoginAuditedWithoutLeakingPassword() throws Exception {
        String sensitiveAttemptPassword = "SuperSecretPlainTextPassword123!";
        LoginRequestDto failedLogin = new LoginRequestDto("admin@calibratesys.edu", sensitiveAttemptPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failedLogin)))
                .andExpect(status().isUnauthorized());

        // Verify audit log has LOGIN_FAILURE
        var failureLogs = auditLogRepository.findByActionOrderByCreatedAtDesc(AuditAction.LOGIN_FAILURE);
        assertFalse(failureLogs.isEmpty(), "Expected at least one LOGIN_FAILURE audit log record");

        AuditLog latest = failureLogs.get(0);
        assertTrue(latest.getDescription().contains("admin@calibratesys.edu"));
        // Confirm the attempted password is NOT stored anywhere in the audit log
        assertFalse(latest.getDescription().contains(sensitiveAttemptPassword), "Password must never be logged in audit description");
        if (latest.getOldValues() != null) {
            assertFalse(latest.getOldValues().contains(sensitiveAttemptPassword));
        }
        if (latest.getNewValues() != null) {
            assertFalse(latest.getNewValues().contains(sensitiveAttemptPassword));
        }
    }

    // =========================================================================
    // API Error Sanitization Tests
    // =========================================================================

    @Test
    @DisplayName("Error Sanitization: Invalid ID type returns sanitized 400 Bad Request without stack traces")
    void testInvalidIdTypeReturnsClean400() throws Exception {
        mockMvc.perform(get("/api/equipment/not-a-number")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid format for parameter 'id'")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("Error Sanitization: Malformed JSON request body returns clean 400 Bad Request")
    void testMalformedJsonReturnsClean400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed-json-payload: true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON request body")));
    }
}
