package com.calibratesys.controller;

import com.calibratesys.dto.*;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Step 16: End-to-End Verification for LAB_STAFF, Multi-Lab Assignments, and Cross-Lab IDOR Boundaries.
 * Covers sections 5, 6, and 7 of the Step 16 specification.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LabStaffAndMultiLabEndToEndTest {

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
    private CalibrationScheduleRepository scheduleRepository;

    @Autowired
    private CalibrationRecordRepository recordRepository;

    @Autowired
    private CalibrationDocumentRepository documentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String staffAToken; // Assigned to Lab A AND Lab B
    private String staffBToken; // Assigned to Lab B ONLY
    private Laboratory labA;
    private Laboratory labB;
    private Equipment eqA;
    private Equipment eqB;
    private CalibrationSchedule schedA;
    private CalibrationSchedule schedB;
    private CalibrationRecord recA;
    private CalibrationRecord recB;
    private CalibrationDocument docA;
    private CalibrationDocument docB;
    private String unique;

    @BeforeEach
    void setUp() {
        unique = String.valueOf(System.currentTimeMillis()).substring(6);

        // 1. Create College
        College college = collegeRepository.save(new College(
                "COL-ML-" + unique, "Multi-Lab College " + unique, null, null, null
        ));

        // 2. Create Laboratories A & B
        labA = laboratoryRepository.save(new Laboratory(
                college, "LAB-A-" + unique, "Alpha Lab " + unique, "Chemistry", "Room 101", LabStatus.ACTIVE
        ));
        labB = laboratoryRepository.save(new Laboratory(
                college, "LAB-B-" + unique, "Beta Lab " + unique, "Physics", "Room 202", LabStatus.ACTIVE
        ));

        // 3. Create Staff A (Dual Lab Staff)
        Staff staffA = staffRepository.save(new Staff(
                "STF-A-" + unique, "Dual", "StaffA", "staff.a." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0111", "Senior Tech", "Science",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        staffAToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staffA));

        // 4. Create Staff B (Single Lab Staff)
        Staff staffB = staffRepository.save(new Staff(
                "STF-B-" + unique, "Single", "StaffB", "staff.b." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0222", "Junior Tech", "Physics",
                SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        staffBToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staffB));

        // 5. Assign Staff A to Lab A AND Lab B
        laboratoryStaffRepository.save(new LaboratoryStaff(labA, staffA, StaffLabRole.LAB_TECHNICIAN, false));
        laboratoryStaffRepository.save(new LaboratoryStaff(labB, staffA, StaffLabRole.PRIMARY_INCHARGE, true));

        // 6. Assign Staff B to Lab B ONLY
        laboratoryStaffRepository.save(new LaboratoryStaff(labB, staffB, StaffLabRole.LAB_TECHNICIAN, false));

        // 7. Create Equipment in Lab A & Lab B
        eqA = equipmentRepository.save(new Equipment(
                labA, "AST-MLA-" + unique, "SN-MLA-" + unique, "Gas Chromatograph " + unique,
                "GC-2000", "Shimadzu", LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(4),
                180, EquipmentStatus.OPERATIONAL
        ));
        eqB = equipmentRepository.save(new Equipment(
                labB, "AST-MLB-" + unique, "SN-MLB-" + unique, "Oscilloscope " + unique,
                "OSC-500", "Tektronix", LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(2),
                90, EquipmentStatus.OPERATIONAL
        ));

        // 8. Create Calibration Schedules
        schedA = scheduleRepository.save(new CalibrationSchedule(
                eqA, LocalDate.now().minusDays(5), LocalDate.now().plusDays(25),
                ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Lab A schedule"
        ));
        schedB = scheduleRepository.save(new CalibrationSchedule(
                eqB, LocalDate.now().minusDays(2), LocalDate.now().plusDays(30),
                ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Lab B schedule"
        ));

        // 9. Create Calibration Records
        CalibrationRecord rA = new CalibrationRecord();
        rA.setEquipment(eqA);
        rA.setCalibrationSchedule(schedA);
        rA.setCalibrationDate(LocalDate.now().minusDays(5));
        rA.setNextDueDate(LocalDate.now().plusDays(175));
        rA.setResult(CalibrationResult.PASSED);
        rA.setCalibratedByType(CalibratedByType.INTERNAL);
        rA.setCalibratorName("Lab A Calibrator");
        recA = recordRepository.save(rA);

        CalibrationRecord rB = new CalibrationRecord();
        rB.setEquipment(eqB);
        rB.setCalibrationSchedule(schedB);
        rB.setCalibrationDate(LocalDate.now().minusDays(2));
        rB.setNextDueDate(LocalDate.now().plusDays(88));
        rB.setResult(CalibrationResult.PASSED);
        rB.setCalibratedByType(CalibratedByType.EXTERNAL_VENDOR);
        rB.setCalibratorName("Vendor Metrology");
        recB = recordRepository.save(rB);

        // 10. Create Calibration Documents
        CalibrationDocument dA = new CalibrationDocument();
        dA.setEquipment(eqA);
        dA.setCalibrationRecord(recA);
        dA.setDocumentName("cert-lab-a.pdf");
        dA.setFilePath("test-uuid-doc-a-" + unique + ".pdf");
        dA.setFileType("application/pdf");
        dA.setFileSizeBytes(1024L);
        dA.setCertificateNumber("CERT-A-" + unique);
        dA.setIssueDate(LocalDate.now().minusDays(5));
        dA.setValidUntil(LocalDate.now().plusDays(175));
        docA = documentRepository.save(dA);

        CalibrationDocument dB = new CalibrationDocument();
        dB.setEquipment(eqB);
        dB.setCalibrationRecord(recB);
        dB.setDocumentName("cert-lab-b.pdf");
        dB.setFilePath("test-uuid-doc-b-" + unique + ".pdf");
        dB.setFileType("application/pdf");
        dB.setFileSizeBytes(2048L);
        dB.setCertificateNumber("CERT-B-" + unique);
        dB.setIssueDate(LocalDate.now().minusDays(2));
        dB.setValidUntil(LocalDate.now().plusDays(88));
        docB = documentRepository.save(dB);

        // 11. Create In-App Notification for Staff B
        notificationRepository.save(new Notification(
                staffB, labB, eqB, schedB, "Schedule Notice", "Calibration notice for Lab B equipment",
                NotificationType.CALIBRATION_UPCOMING, NotificationPriority.MEDIUM
        ));
    }

    // =========================================================================
    // Multi-Lab Access Tests (Section 6)
    // =========================================================================

    @Test
    @DisplayName("Multi-Lab Verification: Staff A (assigned to Lab A & B) can access both Equipment A & B")
    void testStaffACanAccessBothLabAAndLabBEquipment() throws Exception {
        // Staff A accessing Lab A equipment -> 200 OK
        mockMvc.perform(get("/api/equipment/" + eqA.getId())
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eqA.getId()));

        // Staff A accessing Lab B equipment -> 200 OK
        mockMvc.perform(get("/api/equipment/" + eqB.getId())
                        .header("Authorization", "Bearer " + staffAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eqB.getId()));
    }

    @Test
    @DisplayName("Multi-Lab Verification: Staff B (assigned to Lab B only) can access Equipment B but NOT Equipment A")
    void testStaffBCanOnlyAccessLabBEquipment() throws Exception {
        // Staff B accessing assigned Lab B equipment -> 200 OK
        mockMvc.perform(get("/api/equipment/" + eqB.getId())
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eqB.getId()));

        // Staff B accessing unassigned Lab A equipment -> 403 Forbidden
        mockMvc.perform(get("/api/equipment/" + eqA.getId())
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    // =========================================================================
    // Cross-Lab IDOR Security Tests (Section 7)
    // =========================================================================

    @Test
    @DisplayName("Cross-Lab IDOR: Staff B cannot access unassigned Lab A calibration schedule")
    void testStaffBCannotAccessLabASchedule() throws Exception {
        mockMvc.perform(get("/api/calibration-schedules/" + schedA.getId())
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("Cross-Lab IDOR: Staff B cannot access unassigned Lab A calibration record")
    void testStaffBCannotAccessLabARecord() throws Exception {
        mockMvc.perform(get("/api/calibration-records/" + recA.getId())
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("Cross-Lab IDOR: Staff B cannot access unassigned Lab A document metadata or download")
    void testStaffBCannotAccessLabADocument() throws Exception {
        mockMvc.perform(get("/api/calibration-documents/" + docA.getId())
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));

        mockMvc.perform(get("/api/calibration-documents/" + docA.getId() + "/download")
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("Cross-Lab IDOR: Staff B cannot filter reports for unassigned Lab A")
    void testStaffBCannotFilterReportsForUnassignedLab() throws Exception {
        mockMvc.perform(get("/api/reports/calibration-status")
                        .header("Authorization", "Bearer " + staffBToken)
                        .param("laboratoryId", String.valueOf(labA.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    // =========================================================================
    // LAB_STAFF Role Boundary Tests (Section 5)
    // =========================================================================

    @Test
    @DisplayName("Role Boundary: LAB_STAFF cannot perform ADMIN-only write operations")
    void testLabStaffCannotPerformAdminOperations() throws Exception {
        // 1. Create College -> 403 Forbidden
        CollegeRequestDto colDto = new CollegeRequestDto("C-FORBIDDEN", "Forbidden College", null, null, null);
        mockMvc.perform(post("/api/colleges")
                        .header("Authorization", "Bearer " + staffBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(colDto)))
                .andExpect(status().isForbidden());

        // 2. Create Laboratory -> 403 Forbidden
        LaboratoryRequestDto labDto = new LaboratoryRequestDto(labA.getCollege().getId(), "L-FORBIDDEN", "Forbidden Lab", "Physics", "R1", "ACTIVE");
        mockMvc.perform(post("/api/laboratories")
                        .header("Authorization", "Bearer " + staffBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(labDto)))
                .andExpect(status().isForbidden());

        // 3. Create Staff -> 403 Forbidden
        StaffRequestDto staffDto = new StaffRequestDto("STF-FORB", "No", "Way", "noway@cs.edu", null, "Role", "Dept", "LAB_STAFF", "ACTIVE", "Secret123");
        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + staffBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffDto)))
                .andExpect(status().isForbidden());

        // 4. Create Equipment -> 403 Forbidden
        EquipmentRequestDto eqDto = new EquipmentRequestDto(labB.getId(), "TAG-FORB", "SN-FORB", "Equip", "M1", "Mfr", null, null, 90, "OPERATIONAL");
        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + staffBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eqDto)))
                .andExpect(status().isForbidden());

        // 5. Trigger Calibration Alert Generation -> 403 Forbidden
        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LAB_STAFF Permitted Operations: View Dashboard, Notifications, and Scoped Reports")
    void testLabStaffPermittedReadOperations() throws Exception {
        // 1. View Dashboard (scoped to Lab B)
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalLaboratories", is(1))) // Only Lab B visible
                .andExpect(jsonPath("$.laboratories[0].laboratoryId", is(labB.getId().intValue())));

        // 2. View Notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 3. View Scoped Report for Lab B
        mockMvc.perform(get("/api/reports/calibration-status")
                        .header("Authorization", "Bearer " + staffBToken)
                        .param("laboratoryId", String.valueOf(labB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 4. View Document metadata for Lab B
        mockMvc.perform(get("/api/calibration-documents/" + docB.getId())
                        .header("Authorization", "Bearer " + staffBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docB.getId()));
    }
}
