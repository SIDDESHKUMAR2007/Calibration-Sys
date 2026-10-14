package com.calibratesys.controller;

import com.calibratesys.entity.*;
import com.calibratesys.entity.enums.*;
import com.calibratesys.repository.*;
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

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LaboratoryStaffRepository laboratoryStaffRepository;

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

    private String adminToken;
    private String staffToken;
    private Staff adminStaff;
    private Staff scopedStaff;
    private Laboratory labA;
    private Laboratory labB;
    private Laboratory labC;

    private Equipment eqA;
    private Equipment eqB;
    private Equipment eqC;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        adminStaff = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-REP-01", "Global", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0201",
                        "System Admin", "IT", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(adminStaff));

        scopedStaff = staffRepository.save(new Staff(
                "STF-REP-" + unique, "Report", "User" + unique, "report.staff." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0202",
                "Lab Tech", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(scopedStaff));

        College college = collegeRepository.save(new College("COL-REP-" + unique, "Report College " + unique, null, null, null));
        labA = laboratoryRepository.save(new Laboratory(college, "LAB-RA-" + unique, "Report Lab Alpha " + unique, "Physics", "Room 101", LabStatus.ACTIVE));
        labB = laboratoryRepository.save(new Laboratory(college, "LAB-RB-" + unique, "Report Lab Beta " + unique, "Physics", "Room 102", LabStatus.ACTIVE));
        labC = laboratoryRepository.save(new Laboratory(college, "LAB-RC-" + unique, "Report Lab Gamma " + unique, "Chemistry", "Room 103", LabStatus.ACTIVE));

        // Assign scopedStaff to Lab A and Lab B ONLY
        laboratoryStaffRepository.save(new LaboratoryStaff(labA, scopedStaff, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(labB, scopedStaff, StaffLabRole.LAB_TECHNICIAN, false));

        // Create Equipment in Lab A, Lab B, and Lab C
        eqA = equipmentRepository.save(new Equipment(
                labA, "TAG-RA-" + unique, "SN-RA-" + unique, "Oscilloscope \"Pro\", High-Voltage",
                "OSC-2000", "Tektronix", LocalDate.of(2023, 1, 15), LocalDate.of(2023, 2, 1),
                180, EquipmentStatus.OPERATIONAL
        ));

        eqB = equipmentRepository.save(new Equipment(
                labB, "TAG-RB-" + unique, "SN-RB-" + unique, "Digital Multimeter Beta",
                "DMM-500", "Fluke", LocalDate.of(2023, 3, 10), LocalDate.of(2023, 3, 20),
                90, EquipmentStatus.OPERATIONAL
        ));

        eqC = equipmentRepository.save(new Equipment(
                labC, "TAG-RC-" + unique, "SN-RC-" + unique, "Centrifuge Gamma Secret",
                "CF-99", "Eppendorf", LocalDate.of(2023, 5, 5), LocalDate.of(2023, 5, 10),
                365, EquipmentStatus.OPERATIONAL
        ));

        // Add schedules: Overdue schedule for eqA, upcoming schedule for eqB, overdue for eqC
        calibrationScheduleRepository.save(new CalibrationSchedule(
                eqA, LocalDate.now().minusDays(15), LocalDate.now().minusDays(5),
                ScheduleStatus.OVERDUE, SchedulePriority.HIGH, "Overdue check"
        ));

        calibrationScheduleRepository.save(new CalibrationSchedule(
                eqB, LocalDate.now().minusDays(2), LocalDate.now().plusDays(10),
                ScheduleStatus.UPCOMING, SchedulePriority.MEDIUM, "Upcoming check"
        ));

        calibrationScheduleRepository.save(new CalibrationSchedule(
                eqC, LocalDate.now().minusDays(20), LocalDate.now().minusDays(10),
                ScheduleStatus.OVERDUE, SchedulePriority.CRITICAL, "Secret overdue check"
        ));

        // Add records: Passed record for eqA, Failed record for eqB
        calibrationRecordRepository.save(new CalibrationRecord(
                eqA, null, LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                CalibratedByType.INTERNAL, "John Doe", CalibrationResult.PASSED, "0.01", "ISO-17025", "Calibrated OK"
        ));

        calibrationRecordRepository.save(new CalibrationRecord(
                eqB, null, LocalDate.now().minusMonths(3), LocalDate.now().plusDays(10),
                CalibratedByType.EXTERNAL_VENDOR, "Calibration Lab Corp", CalibrationResult.FAILED, "0.05", "NIST", "Requires recalibration"
        ));
    }

    @Test
    @DisplayName("Admin can access all 9 report endpoints successfully")
    void testAdminCanAccessAllReportEndpoints() throws Exception {
        mockMvc.perform(get("/api/reports/calibration-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/reports/upcoming-calibrations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/reports/due-calibrations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/reports/overdue-calibrations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/reports/calibration-history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/reports/equipment")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/reports/laboratories")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/reports/staff-assignments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/reports/compliance-attention")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Lab Staff report data is strictly scoped to assigned laboratories (Lab A and Lab B, never Lab C)")
    void testLabStaffDataScoping() throws Exception {
        // Equipment report for staff: should contain TAG-RA and TAG-RB, but NOT TAG-RC
        mockMvc.perform(get("/api/reports/equipment")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].assetTag", hasItem(eqA.getAssetTag())))
                .andExpect(jsonPath("$.content[*].assetTag", hasItem(eqB.getAssetTag())))
                .andExpect(jsonPath("$.content[*].assetTag", not(hasItem(eqC.getAssetTag()))));

        // Overdue report for staff: should contain eqA schedule, but NOT eqC schedule
        mockMvc.perform(get("/api/reports/overdue-calibrations")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].assetTag", hasItem(eqA.getAssetTag())))
                .andExpect(jsonPath("$.content[*].assetTag", not(hasItem(eqC.getAssetTag()))));

        // Compliance attention for staff: eqC must not appear
        mockMvc.perform(get("/api/reports/compliance-attention")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].assetTag", not(hasItem(eqC.getAssetTag()))));
    }

    @Test
    @DisplayName("Lab Staff explicitly requesting an unassigned laboratory returns 403 Forbidden")
    void testUnauthorizedLaboratoryReturns403() throws Exception {
        mockMvc.perform(get("/api/reports/equipment?laboratoryId=" + labC.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/overdue-calibrations?laboratoryId=" + labC.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/calibration-history?laboratoryId=" + labC.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/compliance-attention?laboratoryId=" + labC.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/reports/equipment"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/overdue-calibrations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid date range where fromDate is after toDate returns 400 Bad Request")
    void testDateRangeValidationReturns400() throws Exception {
        mockMvc.perform(get("/api/reports/equipment?fromDate=2026-12-01&toDate=2026-11-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be after")));

        mockMvc.perform(get("/api/reports/calibration-status?fromDate=2026-12-01&toDate=2026-11-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Invalid sort field returns 400 Bad Request")
    void testInvalidSortFieldReturns400() throws Exception {
        mockMvc.perform(get("/api/reports/equipment?sortBy=nonExistentField")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort field")));

        mockMvc.perform(get("/api/reports/overdue-calibrations?sortBy=hackField")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Invalid sort direction returns 400 Bad Request")
    void testInvalidSortDirectionReturns400() throws Exception {
        mockMvc.perform(get("/api/reports/equipment?sortDirection=SIDEWAYS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid sort direction")));
    }

    @Test
    @DisplayName("Pagination parameters are respected and return PageResponseDto metadata")
    void testPaginationRespected() throws Exception {
        mockMvc.perform(get("/api/reports/equipment?page=0&size=1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("CSV export returns correct headers, content type, disposition, and properly escapes commas and quotes")
    void testCsvExportFormatAndEscaping() throws Exception {
        var result = mockMvc.perform(get("/api/reports/equipment/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"equipment-report-")))
                .andReturn();

        String csvContent = result.getResponse().getContentAsString();
        // Check header row
        assertTrue(csvContent.contains("Equipment ID,Asset Tag,Serial Number,Equipment Name"));

        // Check escaping: eqA has name: Oscilloscope "Pro", High-Voltage
        // In CSV: "Oscilloscope ""Pro"", High-Voltage"
        assertTrue(csvContent.contains("\"Oscilloscope \"\"Pro\"\", High-Voltage\""),
                "Expected escaped quotes and comma in CSV content, got: " + csvContent);
    }

    @Test
    @DisplayName("Lab Staff CSV export strictly enforces data scope and never leaks unassigned Lab C data")
    void testLabStaffCsvExportDataScoping() throws Exception {
        var result = mockMvc.perform(get("/api/reports/equipment/export")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andReturn();

        String csvContent = result.getResponse().getContentAsString();
        assertTrue(csvContent.contains(eqA.getAssetTag()), "Expected Lab A equipment in staff export");
        assertTrue(csvContent.contains(eqB.getAssetTag()), "Expected Lab B equipment in staff export");
        assertFalse(csvContent.contains(eqC.getAssetTag()), "Lab C equipment must NEVER appear in staff export!");
    }

    @Test
    @DisplayName("Audit trail: Normal report viewing does NOT log audit event, but CSV export creates REPORT_EXPORTED event")
    void testAuditTrailOnExportNotViewing() throws Exception {
        long beforeViewCount = auditLogRepository.count();

        // 1. View Report
        mockMvc.perform(get("/api/reports/equipment")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        long afterViewCount = auditLogRepository.count();
        assertEquals(beforeViewCount, afterViewCount, "Normal report viewing must NOT create audit logs");

        // 2. Export Report
        mockMvc.perform(get("/api/reports/equipment/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        long afterExportCount = auditLogRepository.count();
        assertEquals(afterViewCount + 1, afterExportCount, "CSV export MUST create exactly 1 audit log entry");

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog lastLog = logs.get(logs.size() - 1);
        assertEquals(AuditAction.REPORT_EXPORTED, lastLog.getAction());
        assertEquals(AuditEntityType.REPORT, lastLog.getEntityType());
        assertTrue(lastLog.getDescription().contains("Exported Equipment Inventory report to CSV"));
    }
}
