package com.calibratesys.controller;

import com.calibratesys.dto.CalibrationRecordRequestDto;
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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Step 16: Comprehensive Integration Test covering:
 * - Notification Responsible Staff Resolution Hierarchy (Section 8)
 * - Calibration Lifecycle & Schedule-Equipment validation (Sections 9 & 10)
 * - Document Lifecycle & Audit Protection (Section 11)
 * - In-App Notification Lifecycle & User Scoping (Section 12)
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalibrationAndNotificationLifecycleTest {

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

    @Autowired
    private Clock clock;

    private String adminToken;
    private College college;
    private String unique;

    @BeforeEach
    void setUp() {
        unique = String.valueOf(System.currentTimeMillis()).substring(6);

        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        college = collegeRepository.save(new College("COL-LC-" + unique, "Lifecycle College " + unique, null, null, null));
    }

    // =========================================================================
    // Section 8: Primary Contact / Responsible Staff Notification Resolution
    // =========================================================================

    @Test
    @DisplayName("Section 8 Case 1: Active Primary Contact receives calibration alert")
    void testCase1_PrimaryContactReceivesAlert() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(college, "LAB-PC-" + unique, "PC Lab " + unique, "Bio", "R1", LabStatus.ACTIVE));

        Staff primaryContact = staffRepository.save(new Staff(
                "STF-PC-" + unique, "Primary", "Contact", "pc." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret123"), "555-1001", "Lead", "Bio", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        Staff regularStaff = staffRepository.save(new Staff(
                "STF-REG-" + unique, "Regular", "Staff", "reg." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret123"), "555-1002", "Tech", "Bio", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        // Assign both, but mark primaryContact as primaryContact=true
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, primaryContact, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, regularStaff, StaffLabRole.STAFF, false));

        Equipment eq = equipmentRepository.save(new Equipment(
                lab, "AST-C1-" + unique, "SN-C1-" + unique, "Pipette " + unique, "P-100", "Gilson",
                LocalDate.now(clock), LocalDate.now(clock), 30, EquipmentStatus.OPERATIONAL
        ));

        // Create an OVERDUE schedule to trigger alert
        scheduleRepository.save(new CalibrationSchedule(
                eq, LocalDate.now(clock).minusDays(20), LocalDate.now(clock).minusDays(5),
                ScheduleStatus.OVERDUE, SchedulePriority.HIGH, "Overdue for test"
        ));

        // Trigger alert generation
        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify primaryContact received notification
        List<Notification> pcNotifs = notificationRepository.findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(primaryContact.getId());
        assertFalse(pcNotifs.isEmpty(), "Primary contact should receive notification");

        // Verify regular staff did NOT receive notification
        List<Notification> regNotifs = notificationRepository.findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(regularStaff.getId());
        assertTrue(regNotifs.isEmpty(), "Regular staff should not receive notification when primary contact exists");
    }

    @Test
    @DisplayName("Section 8 Case 2: No primary contact -> Calibration coordinator receives alert")
    void testCase2_CoordinatorReceivesAlertWhenNoPrimaryContact() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(college, "LAB-CRD-" + unique, "Coord Lab " + unique, "Chem", "R2", LabStatus.ACTIVE));

        Staff coordinator = staffRepository.save(new Staff(
                "STF-CRD-" + unique, "Calib", "Coord", "coord." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret123"), "555-2001", "Coordinator", "Chem", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        Staff regularStaff = staffRepository.save(new Staff(
                "STF-STF-" + unique, "General", "Staff", "gen." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret123"), "555-2002", "Staff", "Chem", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));

        // Assign without primary contact; coordinator has CALIBRATION_COORDINATOR role
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, coordinator, StaffLabRole.CALIBRATION_COORDINATOR, false));
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, regularStaff, StaffLabRole.STAFF, false));

        Equipment eq = equipmentRepository.save(new Equipment(
                lab, "AST-C2-" + unique, "SN-C2-" + unique, "Spectro " + unique, "S-20", "Thermo",
                LocalDate.now(clock), LocalDate.now(clock), 30, EquipmentStatus.OPERATIONAL
        ));

        scheduleRepository.save(new CalibrationSchedule(
                eq, LocalDate.now(clock).minusDays(15), LocalDate.now(clock).minusDays(2),
                ScheduleStatus.OVERDUE, SchedulePriority.HIGH, "Overdue coordinator test"
        ));

        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<Notification> coordNotifs = notificationRepository.findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(coordinator.getId());
        assertFalse(coordNotifs.isEmpty(), "Coordinator should receive alert when no primary contact exists");

        List<Notification> regNotifs = notificationRepository.findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(regularStaff.getId());
        assertTrue(regNotifs.isEmpty(), "General staff should not receive alert when coordinator exists");
    }

    @Test
    @DisplayName("Section 8 Case 4: Inactive staff members do NOT receive alerts")
    void testCase4_InactiveStaffExcludedFromNotifications() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(college, "LAB-INA-" + unique, "Inactive Lab " + unique, "Phys", "R3", LabStatus.ACTIVE));

        // Inactive staff member as primary contact
        Staff inactiveStaff = staffRepository.save(new Staff(
                "STF-INA-" + unique, "Ex", "Employee", "inactive." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret123"), "555-3001", "Former Lead", "Phys", SystemRole.LAB_STAFF, StaffStatus.INACTIVE
        ));
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, inactiveStaff, StaffLabRole.PRIMARY_INCHARGE, true));

        Equipment eq = equipmentRepository.save(new Equipment(
                lab, "AST-C4-" + unique, "SN-C4-" + unique, "Laser " + unique, "L-50", "Coherent",
                LocalDate.now(clock), LocalDate.now(clock), 30, EquipmentStatus.OPERATIONAL
        ));

        scheduleRepository.save(new CalibrationSchedule(
                eq, LocalDate.now(clock).minusDays(10), LocalDate.now(clock).minusDays(1),
                ScheduleStatus.OVERDUE, SchedulePriority.HIGH, "Overdue inactive test"
        ));

        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<Notification> notifs = notificationRepository.findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(inactiveStaff.getId());
        assertTrue(notifs.isEmpty(), "Inactive staff members must never receive notifications");
    }

    // =========================================================================
    // Section 9 & 10: Calibration Lifecycle and Schedule-Equipment Validation
    // =========================================================================

    @Test
    @DisplayName("Section 10: Attaching a schedule from Equipment A to record for Equipment B is rejected (400 Bad Request)")
    void testScheduleBelongingToOtherEquipmentIsRejected() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(college, "LAB-VAL-" + unique, "Val Lab " + unique, "Bio", "R4", LabStatus.ACTIVE));

        Equipment eqA = equipmentRepository.save(new Equipment(
                lab, "AST-VA-" + unique, "SN-VA-" + unique, "Equipment A " + unique, "M-A", "Mfr", null, null, 90, EquipmentStatus.OPERATIONAL
        ));
        Equipment eqB = equipmentRepository.save(new Equipment(
                lab, "AST-VB-" + unique, "SN-VB-" + unique, "Equipment B " + unique, "M-B", "Mfr", null, null, 90, EquipmentStatus.OPERATIONAL
        ));

        CalibrationSchedule schedA = scheduleRepository.save(new CalibrationSchedule(
                eqA, LocalDate.now(clock), LocalDate.now(clock).plusDays(30), ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Schedule for A"
        ));

        // Attempt to create a record for Equipment B using Schedule A
        CalibrationRecordRequestDto invalidRecord = new CalibrationRecordRequestDto(
                eqB.getId(), schedA.getId(), LocalDate.now(clock), LocalDate.now(clock).plusDays(90),
                CalibratedByType.INTERNAL, "Tech", CalibrationResult.PASSED, null, null, "Mismatch test"
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRecord)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("belongs to equipment")));
    }

    // =========================================================================
    // Section 11: Document Security and Audit Preservation
    // =========================================================================

    @Test
    @DisplayName("Section 11: Document linked to Calibration Record cannot be deleted (preserves compliance history)")
    void testDocumentLinkedToCalibrationRecordCannotBeDeleted() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(college, "LAB-DOCP-" + unique, "Doc Pres Lab " + unique, "Chem", "R5", LabStatus.ACTIVE));
        Equipment eq = equipmentRepository.save(new Equipment(
                lab, "AST-DP-" + unique, "SN-DP-" + unique, "pH Meter " + unique, "PH-1", "Orion", null, null, 90, EquipmentStatus.OPERATIONAL
        ));

        CalibrationRecord rec = new CalibrationRecord();
        rec.setEquipment(eq);
        rec.setCalibrationDate(LocalDate.now(clock).minusDays(5));
        rec.setNextDueDate(LocalDate.now(clock).plusDays(85));
        rec.setResult(CalibrationResult.PASSED);
        rec.setCalibratedByType(CalibratedByType.INTERNAL);
        rec.setCalibratorName("Calibrator");
        rec = recordRepository.save(rec);

        // Upload PDF document linked to Calibration Record
        byte[] pdfBytes = "%PDF-1.5 Sample Certificate".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile certFile = new MockMultipartFile("file", "test-cert-" + unique + ".pdf", "application/pdf", pdfBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/calibration-documents")
                        .file(certFile)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("equipmentId", String.valueOf(eq.getId()))
                        .param("calibrationRecordId", String.valueOf(rec.getId()))
                        .param("documentName", "Audit Preserved Certificate")
                        .param("certificateNumber", "CERT-PRESERVE-" + unique))
                .andExpect(status().isCreated())
                .andReturn();

        Long docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();

        // Attempt to delete document linked to record -> must fail with 400 Bad Request
        mockMvc.perform(delete("/api/calibration-documents/" + docId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("preserved for audit compliance")));
    }

    // =========================================================================
    // Section 12: In-App Notification Lifecycle & Cross-User Security
    // =========================================================================

    @Test
    @DisplayName("Section 12: Notification Lifecycle: Unread count, Mark Single Read, Mark All Read, Cross-User Protection")
    void testNotificationLifecycleAndCrossUserIsolation() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory(college, "LAB-NL-" + unique, "Notif Lab " + unique, "Chem", "R6", LabStatus.ACTIVE));

        Staff staff1 = staffRepository.save(new Staff(
                "STF-N1-" + unique, "User", "One", "u1." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret123"), "555-4001", "Tech", "Chem", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        String staff1Token = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff1));

        Staff staff2 = staffRepository.save(new Staff(
                "STF-N2-" + unique, "User", "Two", "u2." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret123"), "555-4002", "Tech", "Chem", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        String staff2Token = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff2));

        laboratoryStaffRepository.save(new LaboratoryStaff(lab, staff1, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(lab, staff2, StaffLabRole.LAB_TECHNICIAN, false));

        Equipment eq = equipmentRepository.save(new Equipment(
                lab, "AST-NL-" + unique, "SN-NL-" + unique, "Balance " + unique, "B-10", "Mettler", null, null, 90, EquipmentStatus.OPERATIONAL
        ));

        // Create 2 unread notifications for Staff 1
        Notification n1 = notificationRepository.save(new Notification(
                staff1, lab, eq, null, "Notice 1", "Message 1", NotificationType.CALIBRATION_DUE, NotificationPriority.HIGH
        ));
        Notification n2 = notificationRepository.save(new Notification(
                staff1, lab, eq, null, "Notice 2", "Message 2", NotificationType.CALIBRATION_UPCOMING, NotificationPriority.MEDIUM
        ));

        // Check unread count for Staff 1 -> 2
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(2)));

        // Staff 2 attempting to mark Staff 1's notification as read -> 404 or 403 Forbidden
        mockMvc.perform(patch("/api/notifications/" + n1.getId() + "/read")
                        .header("Authorization", "Bearer " + staff2Token))
                .andExpect(status().isForbidden());

        // Staff 1 marks n1 as read -> 200 OK
        mockMvc.perform(patch("/api/notifications/" + n1.getId() + "/read")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read", is(true)));

        // Unread count is now 1
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(1)));

        // Staff 1 marks all read
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk());

        // Unread count is now 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(0)));
    }
}
