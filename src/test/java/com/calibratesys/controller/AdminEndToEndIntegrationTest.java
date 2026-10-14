package com.calibratesys.controller;

import com.calibratesys.dto.*;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.StaffRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Step 16: Comprehensive Admin End-to-End Workflow Integration Test.
 * Validates the complete business sequence from Login through Reporting and Audit Logging.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminEmail;
    private String adminPassword;

    @BeforeEach
    void setUp() {
        adminEmail = "admin@calibratesys.edu";
        adminPassword = "Admin@12345";

        staffRepository.findByEmail(adminEmail)
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", adminEmail,
                        passwordEncoder.encode(adminPassword), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
    }

    @Test
    @DisplayName("Complete Admin End-to-End Workflow: Steps A through O")
    void testCompleteAdminWorkflow() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        // =========================================================================
        // Step A — Login as ADMIN & verify /api/auth/me
        // =========================================================================
        LoginRequestDto loginDto = new LoginRequestDto(adminEmail, adminPassword);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        assertNotNull(token);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(adminEmail))
                .andExpect(jsonPath("$.systemRole").value("ADMIN"));

        // =========================================================================
        // Step B — Create and retrieve a College
        // =========================================================================
        CollegeRequestDto collegeDto = new CollegeRequestDto(
                "COL-E2E-" + unique, "E2E College " + unique, "456 University Way", "contact" + unique + "@e2e.edu", "555-0234"
        );
        MvcResult collegeResult = mockMvc.perform(post("/api/colleges")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collegeDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COL-E2E-" + unique))
                .andReturn();

        Long collegeId = objectMapper.readTree(collegeResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/colleges/" + collegeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(collegeId))
                .andExpect(jsonPath("$.name").value("E2E College " + unique));

        // =========================================================================
        // Step C — Create and retrieve a Laboratory associated with the College
        // =========================================================================
        LaboratoryRequestDto labDto = new LaboratoryRequestDto(
                collegeId, "LAB-E2E-" + unique, "Biochemistry Lab " + unique, "Biochemistry", "Science Hall 202", "ACTIVE"
        );
        MvcResult labResult = mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(labDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.labCode").value("LAB-E2E-" + unique))
                .andReturn();

        Long labId = objectMapper.readTree(labResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/laboratories/" + labId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(labId));

        // =========================================================================
        // Step D — Create a LAB_STAFF member
        // =========================================================================
        StaffRequestDto staffDto = new StaffRequestDto(
                "STF-E2E-" + unique, "Sarah", "Connor", "sarah" + unique + "@calibratesys.edu",
                "555-0899", "Technician Specialist", "Biochemistry", "LAB_STAFF", "ACTIVE", "Password@123"
        );
        MvcResult staffResult = mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffId").value("STF-E2E-" + unique))
                .andReturn();

        Long staffId = objectMapper.readTree(staffResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/staff/" + staffId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(staffId));

        // =========================================================================
        // Step E — Assign LAB_STAFF to Laboratory as PRIMARY_INCHARGE
        // =========================================================================
        LaboratoryStaffRequestDto assignDto = new LaboratoryStaffRequestDto(
                staffId, "PRIMARY_INCHARGE", true, "ACTIVE"
        );
        mockMvc.perform(post("/api/laboratories/" + labId + "/staff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleInLab").value("PRIMARY_INCHARGE"))
                .andExpect(jsonPath("$.primaryContact").value(true));

        mockMvc.perform(get("/api/laboratories/" + labId + "/staff")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].staffId").value(staffId));

        // =========================================================================
        // Step F — Create Equipment in the Laboratory
        // =========================================================================
        EquipmentRequestDto equipDto = new EquipmentRequestDto(
                labId, "AST-E2E-" + unique, "SN-E2E-" + unique, "Chromatography System " + unique,
                "HPLC-1000", "Agilent", LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(6),
                90, "OPERATIONAL"
        );
        MvcResult equipResult = mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetTag").value("AST-E2E-" + unique))
                .andReturn();

        Long equipId = objectMapper.readTree(equipResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/equipment/" + equipId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(equipId));

        // =========================================================================
        // Step G — Create a Calibration Schedule
        // =========================================================================
        LocalDate scheduledDate = LocalDate.now().minusDays(5);
        LocalDate dueDate = LocalDate.now().plusDays(25);
        CalibrationScheduleRequestDto schedDto = new CalibrationScheduleRequestDto(
                equipId, scheduledDate, dueDate, com.calibratesys.entity.enums.SchedulePriority.HIGH, "Quarterly calibration check"
        );
        MvcResult schedResult = mockMvc.perform(post("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(equipId))
                .andReturn();

        Long schedId = objectMapper.readTree(schedResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/calibration-schedules/" + schedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schedId));

        // =========================================================================
        // Step H — Create a Calibration Record against Equipment & Schedule
        // =========================================================================
        LocalDate calibDate = LocalDate.now();
        LocalDate nextDueDate = LocalDate.now().plusDays(90);
        CalibrationRecordRequestDto recordDto = new CalibrationRecordRequestDto(
                equipId, schedId, calibDate, nextDueDate, CalibratedByType.INTERNAL,
                "Dr. John Doe", CalibrationResult.PASSED, "0.005 ppm", "ISO-17025", "Passed all validation tests"
        );
        MvcResult recordResult = mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipmentId").value(equipId))
                .andExpect(jsonPath("$.scheduleId").value(schedId))
                .andExpect(jsonPath("$.result").value("PASSED"))
                .andReturn();

        Long recordId = objectMapper.readTree(recordResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify schedule is now marked COMPLETED
        mockMvc.perform(get("/api/calibration-schedules/" + schedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // =========================================================================
        // Step I — Upload and download a Calibration Certificate (PDF)
        // =========================================================================
        byte[] pdfBytes = "%PDF-1.5 \n1 0 obj <</Type /Catalog>> endobj\ntrailer <</Root 1 0 R>>\n%%EOF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile certFile = new MockMultipartFile("file", "certificate-e2e-" + unique + ".pdf", "application/pdf", pdfBytes);

        MvcResult docResult = mockMvc.perform(multipart("/api/calibration-documents")
                        .file(certFile)
                        .header("Authorization", "Bearer " + token)
                        .param("equipmentId", String.valueOf(equipId))
                        .param("calibrationRecordId", String.valueOf(recordId))
                        .param("documentName", "E2E Certificate " + unique)
                        .param("certificateNumber", "CERT-E2E-" + unique)
                        .param("issueDate", calibDate.toString())
                        .param("validUntil", nextDueDate.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certificateNumber").value("CERT-E2E-" + unique))
                .andReturn();

        Long docId = objectMapper.readTree(docResult.getResponse().getContentAsString()).get("id").asLong();

        // Download document
        mockMvc.perform(get("/api/calibration-documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("E2E_Certificate_" + unique + ".pdf")));

        // =========================================================================
        // Step J — Notification Alert Generation & Verification
        // =========================================================================
        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingSchedulesProcessed").isNumber())
                .andExpect(jsonPath("$.dueSchedulesProcessed").isNumber())
                .andExpect(jsonPath("$.overdueSchedulesProcessed").isNumber());

        // =========================================================================
        // Step K — Compliance Dashboard Overview
        // =========================================================================
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalLaboratories", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.summary.totalEquipment", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.recentCalibrations").isArray());

        mockMvc.perform(get("/api/dashboard/laboratories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // =========================================================================
        // Step L — Global Search for Equipment
        // =========================================================================
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "Chromatography System " + unique))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment[*].id", hasItem(equipId.intValue())));

        // =========================================================================
        // Step M — Retrieve Reports
        // =========================================================================
        mockMvc.perform(get("/api/reports/calibration-history")
                        .header("Authorization", "Bearer " + token)
                        .param("equipmentId", String.valueOf(equipId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].equipmentId").value(equipId));

        // =========================================================================
        // Step N — Export Report as CSV
        // =========================================================================
        mockMvc.perform(get("/api/reports/calibration-history/export")
                        .header("Authorization", "Bearer " + token)
                        .param("equipmentId", String.valueOf(equipId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("calibration-history")));

        // =========================================================================
        // Step O — Audit Trail Verification
        // =========================================================================
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].action", hasItems(
                        "LOGIN_SUCCESS",
                        "COLLEGE_CREATED",
                        "LABORATORY_CREATED",
                        "STAFF_CREATED",
                        "EQUIPMENT_CREATED",
                        "CALIBRATION_SCHEDULE_CREATED",
                        "CALIBRATION_RECORD_CREATED"
                )));
    }
}
