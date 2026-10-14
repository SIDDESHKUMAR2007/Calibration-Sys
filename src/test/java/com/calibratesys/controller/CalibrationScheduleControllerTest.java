package com.calibratesys.controller;

import com.calibratesys.dto.CalibrationScheduleRequestDto;
import com.calibratesys.dto.CalibrationScheduleUpdateRequestDto;
import com.calibratesys.entity.CalibrationRecord;
import com.calibratesys.entity.CalibrationSchedule;
import com.calibratesys.entity.College;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;
import com.calibratesys.entity.enums.EquipmentStatus;
import com.calibratesys.entity.enums.LabStatus;
import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.CalibrationRecordRepository;
import com.calibratesys.repository.CalibrationScheduleRepository;
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

import java.time.Clock;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CalibrationScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalibrationScheduleRepository scheduleRepository;

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
    private Clock clock;

    @Autowired
    private com.calibratesys.repository.LaboratoryStaffRepository laboratoryStaffRepository;

    private String adminToken;
    private String staffToken;
    private Equipment defaultEquipment;
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

        Staff staff = staffRepository.findByEmail("sched.staff@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-SCHED-01", "Bob", "Smith", "sched.staff@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0188",
                        "Lab Technician", "Physics", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff));

        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-SCH-" + unique, "Schedule College " + unique, null, null, null));
        defaultLab = laboratoryRepository.save(new Laboratory(col, "L-SCH-" + unique, "Default Lab " + unique, "Physics", "Room 201", LabStatus.ACTIVE));
        laboratoryStaffRepository.save(new com.calibratesys.entity.LaboratoryStaff(defaultLab, staff, com.calibratesys.entity.enums.StaffLabRole.LAB_TECHNICIAN, false));
        defaultEquipment = equipmentRepository.save(new Equipment(
                defaultLab, "AST-SCH-" + unique, "SN-SCH-" + unique, "Centrifuge 5424R",
                "CF-5424", "Eppendorf", LocalDate.now(clock).minusMonths(6), LocalDate.now(clock).minusMonths(6),
                90, EquipmentStatus.OPERATIONAL
        ));
    }

    @Test
    @DisplayName("1. ADMIN can create calibration schedule successfully (201 Created)")
    void testAdminCanCreateSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        LocalDate scheduledDate = today.plusDays(10);
        LocalDate dueDate = today.plusDays(15);

        CalibrationScheduleRequestDto request = new CalibrationScheduleRequestDto(
                defaultEquipment.getId(), scheduledDate, dueDate, SchedulePriority.HIGH, "Quarterly calibration inspection"
        );

        mockMvc.perform(post("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.equipmentId").value(defaultEquipment.getId()))
                .andExpect(jsonPath("$.equipmentName").value("Centrifuge 5424R"))
                .andExpect(jsonPath("$.assetTag").value(defaultEquipment.getAssetTag()))
                .andExpect(jsonPath("$.laboratoryId").value(defaultLab.getId()))
                .andExpect(jsonPath("$.scheduledDate").value(scheduledDate.toString()))
                .andExpect(jsonPath("$.dueDate").value(dueDate.toString()))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.remarks").value("Quarterly calibration inspection"));
    }

    @Test
    @DisplayName("2. Schedule creation for nonexistent equipment returns 404 Not Found")
    void testScheduleCreationNonexistentEquipment() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationScheduleRequestDto request = new CalibrationScheduleRequestDto(
                999999L, today, today.plusDays(10), SchedulePriority.MEDIUM, "Invalid equipment schedule"
        );

        mockMvc.perform(post("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Equipment not found")));
    }

    @Test
    @DisplayName("3. Schedule creation missing required fields returns 400 Bad Request")
    void testScheduleCreationMissingFields() throws Exception {
        CalibrationScheduleRequestDto request = new CalibrationScheduleRequestDto();

        mockMvc.perform(post("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Schedule creation with due date before scheduled date returns 400 Bad Request")
    void testScheduleCreationDueDateBeforeScheduledDate() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationScheduleRequestDto request = new CalibrationScheduleRequestDto(
                defaultEquipment.getId(), today.plusDays(20), today.plusDays(10), SchedulePriority.MEDIUM, "Invalid date sequence"
        );

        mockMvc.perform(post("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Due date cannot be before scheduled date")));
    }

    @Test
    @DisplayName("5. Both ADMIN and LAB_STAFF can retrieve schedules and single schedule by ID")
    void testScheduleRetrievalAdminAndStaff() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationSchedule schedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(10), ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Test schedule"
        ));

        // ADMIN access
        mockMvc.perform(get("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schedule.getId()))
                .andExpect(jsonPath("$.equipmentId").value(defaultEquipment.getId()));

        // LAB_STAFF access
        mockMvc.perform(get("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schedule.getId()))
                .andExpect(jsonPath("$.equipmentId").value(defaultEquipment.getId()));
    }

    @Test
    @DisplayName("6. Retrieve schedules by equipment ID and verify 404 for nonexistent equipment")
    void testScheduleRetrievalByEquipmentId() throws Exception {
        LocalDate today = LocalDate.now(clock);
        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(10), ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Equip Schedule"
        ));

        mockMvc.perform(get("/api/equipment/" + defaultEquipment.getId() + "/calibration-schedules")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].equipmentId").value(defaultEquipment.getId()));

        mockMvc.perform(get("/api/equipment/999999/calibration-schedules")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Equipment not found")));
    }

    @Test
    @DisplayName("7. Deterministic date status calculation (OVERDUE, DUE, UPCOMING, SCHEDULED)")
    void testDeterministicDateStatusCalculation() throws Exception {
        LocalDate today = LocalDate.now(clock);

        // Overdue (due 5 days ago)
        CalibrationSchedule sOverdue = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today.minusDays(10), today.minusDays(5), ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Overdue Item"
        ));

        // Due today
        CalibrationSchedule sDue = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today.minusDays(5), today, ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Due Today Item"
        ));

        // Upcoming (due in 15 days, <= 30 threshold)
        CalibrationSchedule sUpcoming = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(15), ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Upcoming Item"
        ));

        // Scheduled (due in 60 days, > 30 threshold)
        CalibrationSchedule sScheduled = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(60), ScheduleStatus.SCHEDULED, SchedulePriority.LOW, "Scheduled Item"
        ));

        mockMvc.perform(get("/api/calibration-schedules/" + sOverdue.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OVERDUE"));

        mockMvc.perform(get("/api/calibration-schedules/" + sDue.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUE"));

        mockMvc.perform(get("/api/calibration-schedules/" + sUpcoming.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPCOMING"));

        mockMvc.perform(get("/api/calibration-schedules/" + sScheduled.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("8. Filtering schedules by status, priority, equipment, laboratory, and date range")
    void testFilteringSchedules() throws Exception {
        LocalDate today = LocalDate.now(clock);

        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today.minusDays(10), today.minusDays(2), ScheduleStatus.SCHEDULED, SchedulePriority.CRITICAL, "Filter Overdue Critical"
        ));
        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(10), ScheduleStatus.SCHEDULED, SchedulePriority.LOW, "Filter Upcoming Low"
        ));

        // Filter by priority CRITICAL and equipment
        mockMvc.perform(get("/api/calibration-schedules?equipmentId=" + defaultEquipment.getId() + "&priority=CRITICAL")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].priority").value("CRITICAL"));

        // Filter by laboratoryId
        mockMvc.perform(get("/api/calibration-schedules?laboratoryId=" + defaultLab.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Filter by date range and equipment
        mockMvc.perform(get("/api/calibration-schedules?equipmentId=" + defaultEquipment.getId() + "&fromDate=" + today.plusDays(5) + "&toDate=" + today.plusDays(20))
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].priority").value("LOW"));

        // Filter by status OVERDUE and equipment
        mockMvc.perform(get("/api/calibration-schedules?equipmentId=" + defaultEquipment.getId() + "&status=OVERDUE")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"));
    }

    @Test
    @DisplayName("9. Convenience endpoints for upcoming, due, and overdue schedules")
    void testConvenienceEndpoints() throws Exception {
        LocalDate today = LocalDate.now(clock);

        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today.minusDays(10), today.minusDays(2), ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Overdue 1"
        ));
        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today, ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Due Today 1"
        ));
        scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(20), ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Upcoming 1"
        ));

        mockMvc.perform(get("/api/calibration-schedules/overdue")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OVERDUE"));

        mockMvc.perform(get("/api/calibration-schedules/due")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DUE"));

        mockMvc.perform(get("/api/calibration-schedules/upcoming")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("UPCOMING"));
    }

    @Test
    @DisplayName("10. ADMIN can update schedule details (200 OK)")
    void testAdminCanUpdateSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationSchedule schedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(10), ScheduleStatus.SCHEDULED, SchedulePriority.LOW, "Initial Remarks"
        ));

        CalibrationScheduleUpdateRequestDto updateRequest = new CalibrationScheduleUpdateRequestDto(
                today.plusDays(1), today.plusDays(15), SchedulePriority.CRITICAL, null, "Updated Critical Remarks"
        );

        mockMvc.perform(put("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.remarks").value("Updated Critical Remarks"));
    }

    @Test
    @DisplayName("11. ADMIN can cancel schedule (200 OK, status CANCELLED)")
    void testAdminCanCancelSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationSchedule schedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(10), ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Active Schedule"
        ));

        mockMvc.perform(patch("/api/calibration-schedules/" + schedule.getId() + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("12. ADMIN can delete non-historical schedule (204 No Content)")
    void testAdminCanDeleteSafeSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationSchedule schedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(10), ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Deletable Schedule"
        ));

        mockMvc.perform(delete("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(scheduleRepository.existsById(schedule.getId()));
    }

    @Test
    @DisplayName("13. Delete is blocked with 409 Conflict when associated calibration records exist")
    void testDeleteBlockedWhenCalibrationRecordExists() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationSchedule schedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today.minusDays(10), today.minusDays(2), ScheduleStatus.SCHEDULED, SchedulePriority.HIGH, "Historical Schedule"
        ));

        CalibrationRecord record = new CalibrationRecord(
                defaultEquipment, schedule, today.minusDays(2), today.plusDays(88),
                CalibratedByType.INTERNAL, "John Calibrator", CalibrationResult.PASSED,
                null, null, "Historical record"
        );
        calibrationRecordRepository.save(record);

        mockMvc.perform(delete("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("associated calibration records exist")));
    }

    @Test
    @DisplayName("14. Delete is blocked with 409 Conflict when schedule is marked COMPLETED")
    void testDeleteBlockedWhenCompleted() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationSchedule schedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today.minusDays(10), today.minusDays(2), ScheduleStatus.COMPLETED, SchedulePriority.HIGH, "Completed Schedule"
        ));

        mockMvc.perform(delete("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Completed schedules must be preserved")));
    }

    @Test
    @DisplayName("15. LAB_STAFF receives 403 Forbidden on write, update, cancel, and delete operations")
    void testLabStaffForbiddenOnWriteOperations() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationSchedule schedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, today, today.plusDays(10), ScheduleStatus.SCHEDULED, SchedulePriority.MEDIUM, "Lab Staff Target"
        ));

        CalibrationScheduleRequestDto createDto = new CalibrationScheduleRequestDto(
                defaultEquipment.getId(), today, today.plusDays(10), SchedulePriority.MEDIUM, "Forbidden Create"
        );
        CalibrationScheduleUpdateRequestDto updateDto = new CalibrationScheduleUpdateRequestDto(
                today, today.plusDays(15), SchedulePriority.HIGH, null, "Forbidden Update"
        );

        // POST by LAB_STAFF
        mockMvc.perform(post("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());

        // PUT by LAB_STAFF
        mockMvc.perform(put("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());

        // PATCH cancel by LAB_STAFF
        mockMvc.perform(patch("/api/calibration-schedules/" + schedule.getId() + "/cancel")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        // DELETE by LAB_STAFF
        mockMvc.perform(delete("/api/calibration-schedules/" + schedule.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("16. Unauthenticated requests to calibration schedule endpoints receive 401 Unauthorized")
    void testUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/calibration-schedules"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/calibration-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
