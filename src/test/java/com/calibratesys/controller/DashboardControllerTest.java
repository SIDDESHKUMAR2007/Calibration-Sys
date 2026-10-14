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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CalibrationDocumentRepository calibrationDocumentRepository;

    @Autowired
    private CalibrationRecordRepository calibrationRecordRepository;

    @Autowired
    private CalibrationScheduleRepository scheduleRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private LaboratoryStaffRepository laboratoryStaffRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private Clock clock;

    private String adminToken;
    private String staff1Token;
    private String unassignedStaffToken;
    private Staff adminStaff;
    private Staff staff1;
    private Staff unassignedStaff;

    private College college1;
    private College college2;
    private Laboratory labA;
    private Laboratory labB;
    private Laboratory labC;

    private Equipment equip1;
    private Equipment equip2;
    private Equipment equip3;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(clock);

        notificationRepository.deleteAll();

        adminStaff = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(adminStaff));

        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        staff1 = staffRepository.save(new Staff(
                "STF-DASH-1-" + unique, "Alice", "Scientist", "alice.dash." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0111",
                "Senior Coordinator", "Biochemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        staff1Token = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff1));

        unassignedStaff = staffRepository.save(new Staff(
                "STF-DASH-2-" + unique, "Charlie", "NoLab", "charlie.dash." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0222",
                "General Staff", "Chemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        unassignedStaffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(unassignedStaff));

        college1 = collegeRepository.save(new College("C-DASH-1-" + unique, "Dash College 1 " + unique, null, null, null));
        college2 = collegeRepository.save(new College("C-DASH-2-" + unique, "Dash College 2 " + unique, null, null, null));

        labA = laboratoryRepository.save(new Laboratory(college1, "L-DASH-A-" + unique, "Lab A " + unique, "Biochemistry", "Room 101", LabStatus.ACTIVE));
        labB = laboratoryRepository.save(new Laboratory(college1, "L-DASH-B-" + unique, "Lab B " + unique, "Microbiology", "Room 102", LabStatus.ACTIVE));
        labC = laboratoryRepository.save(new Laboratory(college2, "L-DASH-C-" + unique, "Lab C " + unique, "Physics", "Room 201", LabStatus.ACTIVE));

        // Staff 1 is assigned to Lab A and Lab B, but NOT Lab C
        laboratoryStaffRepository.save(new LaboratoryStaff(labA, staff1, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(labB, staff1, StaffLabRole.LAB_TECHNICIAN, false));

        equip1 = equipmentRepository.save(new Equipment(
                labA, "AST-DASH-1-" + unique, "SN-DASH-1-" + unique, "Microscope A",
                "OPT-400", "Olympus", today.minusMonths(12), today.minusMonths(6),
                180, EquipmentStatus.OPERATIONAL
        ));

        equip2 = equipmentRepository.save(new Equipment(
                labB, "AST-DASH-2-" + unique, "SN-DASH-2-" + unique, "Centrifuge B",
                "CF-200", "Eppendorf", today.minusMonths(8), today.minusMonths(2),
                90, EquipmentStatus.UNDER_MAINTENANCE
        ));

        equip3 = equipmentRepository.save(new Equipment(
                labC, "AST-DASH-3-" + unique, "SN-DASH-3-" + unique, "Spectrometer C",
                "SP-500", "Shimadzu", today.minusMonths(10), today.minusMonths(4),
                180, EquipmentStatus.CALIBRATION_DUE
        ));
    }

    @Test
    @DisplayName("Security: Unauthenticated request to /api/dashboard returns 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Section 16: Multi-Lab Staff scoping - Staff A sees Lab A + Lab B equipment, excludes Lab C")
    void multiLabStaff_AggregatesAssignedLabsAndExcludesUnassignedLab() throws Exception {
        // Staff A should see equip1 (Lab A) and equip2 (Lab B) -> totalEquipment = 2
        // Staff A should NOT see equip3 (Lab C)
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLaboratories", is(2)))
                .andExpect(jsonPath("$.totalEquipment", is(2)))
                .andExpect(jsonPath("$.operationalEquipment", is(1)))
                .andExpect(jsonPath("$.equipmentUnderMaintenance", is(1)))
                .andExpect(jsonPath("$.equipmentCalibrationDue", is(0))); // equip3 is in Lab C, so 0 for Staff A
    }

    @Test
    @DisplayName("Section 15 & 17: Admin receives organization-wide summary including all colleges and labs")
    void admin_ReceivesOrganizationWideSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLaboratories", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalEquipment", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.operationalEquipment", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.equipmentUnderMaintenance", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.equipmentCalibrationDue", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Section 17: LAB_STAFF cannot access unassigned laboratoryId filter (Returns 403 Forbidden)")
    void labStaff_AccessUnassignedLaboratory_ReturnsForbidden403() throws Exception {
        // Lab C is not assigned to staff1
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + staff1Token)
                        .param("laboratoryId", labC.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Section 17: LAB_STAFF can filter by assigned laboratoryId")
    void labStaff_FilterByAssignedLaboratory_ReturnsScopedSummary() throws Exception {
        // Lab A is assigned to staff1
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + staff1Token)
                        .param("laboratoryId", labA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLaboratories", is(1)))
                .andExpect(jsonPath("$.totalEquipment", is(1)))
                .andExpect(jsonPath("$.operationalEquipment", is(1)))
                .andExpect(jsonPath("$.equipmentUnderMaintenance", is(0)));
    }

    @Test
    @DisplayName("Section 4 & 18: Calibration status breakdown reflects date-driven rules with Clock")
    void calibrationStatusSummary_ReturnsCorrectCounts() throws Exception {
        // Create 1 OVERDUE schedule (dueDate in the past) for equip1 (Lab A)
        scheduleRepository.save(new CalibrationSchedule(
                equip1, today.minusDays(20), today.minusDays(5),
                ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Overdue check"
        ));

        // Create 1 DUE schedule (dueDate today) for equip2 (Lab B)
        scheduleRepository.save(new CalibrationSchedule(
                equip2, today.minusDays(10), today,
                ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Due today check"
        ));

        // Create 1 UPCOMING schedule (dueDate today + 10) for equip3 (Lab C)
        scheduleRepository.save(new CalibrationSchedule(
                equip3, today, today.plusDays(10),
                ScheduleStatus.SCHEDULED, SchedulePriority.LOW, "Upcoming check"
        ));

        // For Staff 1 (Lab A and Lab B): overdue = 1, due = 1, upcoming = 0 (since Lab C is excluded)
        mockMvc.perform(get("/api/dashboard/calibration-status")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue", is(1)))
                .andExpect(jsonPath("$.due", is(1)))
                .andExpect(jsonPath("$.upcoming", is(0)))
                .andExpect(jsonPath("$.total", is(2)));

        // For Admin: overdue >= 1, due >= 1, upcoming >= 1
        mockMvc.perform(get("/api/dashboard/calibration-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.due", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.upcoming", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Section 5: Equipment status summary correctly breaks down statuses")
    void equipmentStatusSummary_ReturnsCorrectBreakdown() throws Exception {
        mockMvc.perform(get("/api/dashboard/equipment-status")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operational", is(1)))
                .andExpect(jsonPath("$.underMaintenance", is(1)))
                .andExpect(jsonPath("$.calibrationDue", is(0)))
                .andExpect(jsonPath("$.total", is(2)));
    }

    @Test
    @DisplayName("Section 6, 7 & 8: Upcoming, Due, and Overdue list endpoints return correct items")
    void upcomingDueOverdueLists_ReturnCorrectItems() throws Exception {
        CalibrationSchedule overdueSched = scheduleRepository.save(new CalibrationSchedule(
                equip1, today.minusDays(15), today.minusDays(3),
                ScheduleStatus.SCHEDULED, SchedulePriority.CRITICAL, "Overdue inspection"
        ));

        CalibrationSchedule dueSched = scheduleRepository.save(new CalibrationSchedule(
                equip2, today.minusDays(7), today,
                ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Due calibration"
        ));

        // Overdue calibrations for Staff 1
        mockMvc.perform(get("/api/dashboard/overdue-calibrations")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scheduleId", is(overdueSched.getId().intValue())))
                .andExpect(jsonPath("$[0].equipmentName", is("Microscope A")))
                .andExpect(jsonPath("$[0].status", is("OVERDUE")))
                .andExpect(jsonPath("$[0].daysOverdue", is(3)))
                .andExpect(jsonPath("$[0].responsibleStaffName", notNullValue()));

        // Due calibrations for Staff 1
        mockMvc.perform(get("/api/dashboard/due-calibrations")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scheduleId", is(dueSched.getId().intValue())))
                .andExpect(jsonPath("$[0].equipmentName", is("Centrifuge B")))
                .andExpect(jsonPath("$[0].status", is("DUE")))
                .andExpect(jsonPath("$[0].daysOverdue", is(0)));
    }

    @Test
    @DisplayName("Section 9 & 30: Recent calibration activity shows latest records and document presence indicator")
    void recentCalibrations_ReturnsNewestFirstWithDocumentIndicator() throws Exception {
        // Record 1 (older) without document
        CalibrationRecord rec1 = calibrationRecordRepository.save(new CalibrationRecord(
                equip1, null, today.minusDays(10), today.plusDays(170),
                CalibratedByType.INTERNAL, "Dr. Internal", CalibrationResult.PASSED,
                "0.01mm", "ISO-100", "Standard calibration"
        ));

        // Record 2 (newer) with document
        CalibrationRecord rec2 = calibrationRecordRepository.save(new CalibrationRecord(
                equip1, null, today.minusDays(2), today.plusDays(178),
                CalibratedByType.EXTERNAL_VENDOR, "Calibration Lab Corp", CalibrationResult.PASSED,
                "0.005mm", "ISO-200", "Certified external calibration"
        ));

        calibrationDocumentRepository.save(new CalibrationDocument(
                equip1, rec2, "Cert-2026.pdf", "CERT-8899", "uploads/test.pdf",
                "application/pdf", 1024L, today.minusDays(2), today.plusDays(178)
        ));

        mockMvc.perform(get("/api/dashboard/recent-calibrations")
                        .header("Authorization", "Bearer " + staff1Token)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Newest record first (rec2)
                .andExpect(jsonPath("$[0].recordId", is(rec2.getId().intValue())))
                .andExpect(jsonPath("$[0].calibratorName", is("Calibration Lab Corp")))
                .andExpect(jsonPath("$[0].hasCertificate", is(true)))
                .andExpect(jsonPath("$[0].documentCount", is(1)))
                // Older record second (rec1)
                .andExpect(jsonPath("$[1].recordId", is(rec1.getId().intValue())))
                .andExpect(jsonPath("$[1].hasCertificate", is(false)))
                .andExpect(jsonPath("$[1].documentCount", is(0)));
    }

    @Test
    @DisplayName("Section 10 & 11: Laboratory summary and overdue view identify labs with overdue equipment")
    void laboratorySummaries_AndOverdueFilter_IdentifiesOverdueLabs() throws Exception {
        // Create an OVERDUE schedule for Lab A (equip1)
        scheduleRepository.save(new CalibrationSchedule(
                equip1, today.minusDays(20), today.minusDays(5),
                ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Overdue Lab A"
        ));

        // Staff 1 gets laboratory summaries for Lab A and Lab B
        mockMvc.perform(get("/api/dashboard/laboratories")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.laboratoryCode == '" + labA.getLabCode() + "')].overdueCount", hasItem(1)))
                .andExpect(jsonPath("$[?(@.laboratoryCode == '" + labB.getLabCode() + "')].overdueCount", hasItem(0)));

        // Dedicated Overdue Alert View: only Lab A should appear
        mockMvc.perform(get("/api/dashboard/laboratories/overdue")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].laboratoryCode", is(labA.getLabCode())))
                .andExpect(jsonPath("$[0].overdueCount", is(1)))
                .andExpect(jsonPath("$[0].hasOverdue", is(true)));
    }

    @Test
    @DisplayName("Section 27: Staff with zero laboratory assignments receives empty dashboard gracefully")
    void unassignedStaff_ReceivesEmptyDashboardGracefully() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + unassignedStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLaboratories", is(0)))
                .andExpect(jsonPath("$.totalEquipment", is(0)))
                .andExpect(jsonPath("$.operationalEquipment", is(0)))
                .andExpect(jsonPath("$.upcomingSchedules", is(0)))
                .andExpect(jsonPath("$.dueSchedules", is(0)))
                .andExpect(jsonPath("$.overdueSchedules", is(0)));

        mockMvc.perform(get("/api/dashboard/laboratories")
                        .header("Authorization", "Bearer " + unassignedStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Section 14: Unified GET /api/dashboard returns complete aggregated dashboard response")
    void unifiedDashboardEndpoint_ReturnsAllAggregatedSections() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", notNullValue()))
                .andExpect(jsonPath("$.calibrationStatus", notNullValue()))
                .andExpect(jsonPath("$.equipmentStatus", notNullValue()))
                .andExpect(jsonPath("$.upcomingCalibrations", notNullValue()))
                .andExpect(jsonPath("$.dueCalibrations", notNullValue()))
                .andExpect(jsonPath("$.overdueCalibrations", notNullValue()))
                .andExpect(jsonPath("$.recentCalibrations", notNullValue()))
                .andExpect(jsonPath("$.laboratories", notNullValue()))
                .andExpect(jsonPath("$.recentNotifications", notNullValue()))
                .andExpect(jsonPath("$.unreadNotificationCount", notNullValue()));
    }
}
