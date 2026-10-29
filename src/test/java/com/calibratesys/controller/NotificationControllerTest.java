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

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CalibrationScheduleRepository scheduleRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private LaboratoryStaffRepository laboratoryStaffRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String adminToken;
    private String staff1Token;
    private String staff2Token;
    private Staff staff1;
    private Staff staff2;
    private Laboratory defaultLab;
    private Equipment defaultEquipment;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        staff1 = staffRepository.save(new Staff(
                "STF-NOTIF-1-" + unique, "Alice", "Smith", "alice." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0111",
                "Senior Coordinator", "Biochemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        staff1Token = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff1));

        staff2 = staffRepository.save(new Staff(
                "STF-NOTIF-2-" + unique, "Bob", "Jones", "bob." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0222",
                "Lab Technician", "Biochemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        staff2Token = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff2));

        College college = collegeRepository.save(new College("C-NOTIF-" + unique, "Notification College " + unique, null, null, null));
        defaultLab = laboratoryRepository.save(new Laboratory(college, "L-NOTIF-" + unique, "Notification Lab " + unique, "Biochemistry", "Room 501", LabStatus.ACTIVE));

        defaultEquipment = equipmentRepository.save(new Equipment(
                defaultLab, "AST-NOTIF-" + unique, "SN-NOTIF-" + unique, "Spectrometer",
                "SPEC-900", "PerkinElmer", LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(6),
                180, EquipmentStatus.OPERATIONAL
        ));
    }

    @Test
    @DisplayName("Recipient Resolution: Case A - Primary Contact receives alert")
    void recipientResolution_PrimaryContact_ReceivesAlert() throws Exception {
        // Staff 1 is primary contact; Staff 2 is lab technician
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff1, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff2, StaffLabRole.LAB_TECHNICIAN, false));

        // Create an OVERDUE schedule for defaultEquipment
        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, LocalDate.now().minusDays(10), LocalDate.now().minusDays(5),
                ScheduleStatus.OVERDUE, SchedulePriority.HIGH, "Overdue check"
        ));

        // ADMIN triggers alert generation
        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueSchedulesProcessed").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.notificationsCreated").value(greaterThanOrEqualTo(1)));

        // Staff 1 (Primary contact) should receive the notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].notificationType").value("CALIBRATION_OVERDUE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));

        // Staff 2 should NOT receive notification because primary contact took precedence
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Recipient Resolution: Case B - No Primary Contact: Fallback to specialized staff (Coordinator & Tech)")
    void recipientResolution_NoPrimaryContact_FallbackToSpecializedStaff() throws Exception {
        // Staff 1 is CALIBRATION_COORDINATOR; Staff 2 is LAB_TECHNICIAN (neither is primary)
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff1, StaffLabRole.CALIBRATION_COORDINATOR, false));
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff2, StaffLabRole.LAB_TECHNICIAN, false));

        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, LocalDate.now().minusDays(10), LocalDate.now(),
                ScheduleStatus.DUE, SchedulePriority.HIGH, "Due today"
        ));

        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueSchedulesProcessed").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.notificationsCreated").value(greaterThanOrEqualTo(2)));

        // Both Staff 1 and Staff 2 should receive the notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].notificationType").value("CALIBRATION_DUE"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].notificationType").value("CALIBRATION_DUE"));
    }

    @Test
    @DisplayName("Recipient Resolution: Case C - Inactive assignment is excluded")
    void recipientResolution_InactiveAssignment_IsExcluded() throws Exception {
        // Staff 1 has INACTIVE primary assignment
        LaboratoryStaff ls1 = new LaboratoryStaff(defaultLab, staff1, StaffLabRole.PRIMARY_INCHARGE, true);
        ls1.setStatus("INACTIVE");
        laboratoryStaffRepository.save(ls1);

        // Staff 2 has ACTIVE coordinator assignment
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff2, StaffLabRole.CALIBRATION_COORDINATOR, false));

        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, LocalDate.now().minusDays(10), LocalDate.now().plusDays(5),
                ScheduleStatus.UPCOMING, SchedulePriority.MEDIUM, "Upcoming test"
        ));

        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Inactive Staff 1 should get 0 notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Active Staff 2 receives notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].notificationType").value("CALIBRATION_UPCOMING"));
    }

    @Test
    @DisplayName("Recipient Resolution: Staff with multiple labs only receives alerts for their assigned equipment lab")
    void recipientResolution_MultiLabStaff_ScopedToLab() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-OTHER-" + unique, "Other College", null, null, null));
        Laboratory otherLab = laboratoryRepository.save(new Laboratory(col, "L-OTHER-" + unique, "Other Lab", "Physics", "Room 101", LabStatus.ACTIVE));

        Equipment otherEquipment = equipmentRepository.save(new Equipment(
                otherLab, "AST-OTHER-" + unique, "SN-OTHER-" + unique, "Oscilloscope",
                "OSC-10", "Tektronix", LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(4),
                90, EquipmentStatus.OPERATIONAL
        ));

        // Staff 1 is in defaultLab only; Staff 2 is in otherLab only
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff1, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(otherLab, staff2, StaffLabRole.PRIMARY_INCHARGE, true));

        // Schedule for otherEquipment in otherLab
        scheduleRepository.save(new CalibrationSchedule(
                otherEquipment, LocalDate.now().minusDays(10), LocalDate.now().minusDays(2),
                ScheduleStatus.OVERDUE, SchedulePriority.HIGH, "Overdue check"
        ));

        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Staff 1 (in defaultLab) must NOT receive notification for otherLab's equipment
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Staff 2 (in otherLab) MUST receive notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Duplicate Prevention: Repeated alert generation is idempotent")
    void duplicatePrevention_RepeatedGeneration_IsIdempotent() throws Exception {
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff1, StaffLabRole.PRIMARY_INCHARGE, true));
        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, LocalDate.now().minusDays(10), LocalDate.now().minusDays(3),
                ScheduleStatus.OVERDUE, SchedulePriority.HIGH, "Overdue check"
        ));

        // First generation -> creates notification
        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationsCreated").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.notificationsSkippedAsDuplicates").value(0));

        // Second generation -> skips creating duplicate
        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationsCreated").value(0))
                .andExpect(jsonPath("$.notificationsSkippedAsDuplicates").value(greaterThanOrEqualTo(1)));

        // Total notifications for staff1 remains 1
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Completed and Cancelled schedules do not generate notifications")
    void completedAndCancelledSchedules_DoNotGenerateNotifications() throws Exception {
        laboratoryStaffRepository.save(new LaboratoryStaff(defaultLab, staff1, StaffLabRole.PRIMARY_INCHARGE, true));

        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, LocalDate.now().minusDays(20), LocalDate.now().minusDays(10),
                ScheduleStatus.COMPLETED, SchedulePriority.HIGH, "Completed"
        ));
        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, LocalDate.now().minusDays(20), LocalDate.now().minusDays(10),
                ScheduleStatus.CANCELLED, SchedulePriority.HIGH, "Cancelled"
        ));

        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify Staff 1 receives 0 notifications because defaultEquipment only has COMPLETED / CANCELLED schedules
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("User Isolation: Staff A cannot view or modify Staff B's notifications")
    void userIsolation_StaffCannotAccessOtherStaffNotifications() throws Exception {
        // Create a direct notification for Staff 2
        Notification notifForStaff2 = notificationRepository.save(new Notification(
                staff2, defaultLab, defaultEquipment, null,
                "Test Title", "Test Message", NotificationType.GENERAL_NOTICE, NotificationPriority.LOW
        ));

        // Staff 1 requests notifications list -> should not see Staff 2's notification
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Staff 1 requests Staff 2's notification by ID -> 403 Forbidden
        mockMvc.perform(get("/api/notifications/" + notifForStaff2.getId())
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isForbidden());

        // Staff 1 attempts to mark Staff 2's notification as read -> 403 Forbidden
        mockMvc.perform(patch("/api/notifications/" + notifForStaff2.getId() + "/read")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isForbidden());

        // Verify Staff 2's notification remains unread
        Notification refreshed = notificationRepository.findById(notifForStaff2.getId()).orElseThrow();
        assertEquals(false, refreshed.isRead());
    }

    @Test
    @DisplayName("Notification Lifecycle: Mark read, Unread count, and Mark all read")
    void notificationLifecycle_Success() throws Exception {
        Notification n1 = notificationRepository.save(new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Notice 1", "Body 1", NotificationType.CALIBRATION_UPCOMING, NotificationPriority.MEDIUM
        ));
        Notification n2 = notificationRepository.save(new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Notice 2", "Body 2", NotificationType.CALIBRATION_DUE, NotificationPriority.HIGH
        ));

        // Check unread count: should be 2
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        // Mark n1 as read
        mockMvc.perform(patch("/api/notifications/" + n1.getId() + "/read")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(n1.getId()))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // Unread count should now be 1
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        // Mark all as read
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1));

        // Unread count should now be 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("Security: Unauthenticated access returns 401 Unauthorized")
    void security_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/notifications/generate-calibration-alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: LAB_STAFF cannot trigger alert generation (403 Forbidden)")
    void security_LabStaffCannotTriggerAlertGeneration() throws Exception {
        mockMvc.perform(post("/api/notifications/generate-calibration-alerts")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Filter isRead=true: Returns only read notifications")
    void getNotifications_WithIsReadTrue_ReturnsOnlyReadNotifications() throws Exception {
        Notification unreadNotif = notificationRepository.save(new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Unread Alert", "This is unread", NotificationType.CALIBRATION_UPCOMING, NotificationPriority.HIGH
        ));

        Notification readNotif = new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Read Alert", "This is read", NotificationType.CALIBRATION_DUE, NotificationPriority.MEDIUM
        );
        readNotif.setRead(true);
        readNotif = notificationRepository.save(readNotif);

        mockMvc.perform(get("/api/notifications")
                        .param("isRead", "true")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(readNotif.getId()))
                .andExpect(jsonPath("$[0].read").value(true));
    }

    @Test
    @DisplayName("Filter isRead=false: Returns only unread notifications")
    void getNotifications_WithIsReadFalse_ReturnsOnlyUnreadNotifications() throws Exception {
        Notification unreadNotif = notificationRepository.save(new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Unread Alert", "This is unread", NotificationType.CALIBRATION_UPCOMING, NotificationPriority.HIGH
        ));

        Notification readNotif = new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Read Alert", "This is read", NotificationType.CALIBRATION_DUE, NotificationPriority.MEDIUM
        );
        readNotif.setRead(true);
        readNotif = notificationRepository.save(readNotif);

        mockMvc.perform(get("/api/notifications")
                        .param("isRead", "false")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(unreadNotif.getId()))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @DisplayName("Filter omitted isRead: Returns both read and unread notifications")
    void getNotifications_WithOmittedIsRead_ReturnsAllNotifications() throws Exception {
        Notification unreadNotif = notificationRepository.save(new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Unread Alert", "This is unread", NotificationType.CALIBRATION_UPCOMING, NotificationPriority.HIGH
        ));

        Notification readNotif = new Notification(
                staff1, defaultLab, defaultEquipment, null,
                "Read Alert", "This is read", NotificationType.CALIBRATION_DUE, NotificationPriority.MEDIUM
        );
        readNotif.setRead(true);
        readNotif = notificationRepository.save(readNotif);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + staff1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
