package com.calibratesys.controller;

import com.calibratesys.dto.CalibrationRecordRequestDto;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CalibrationRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalibrationRecordRepository recordRepository;

    @Autowired
    private CalibrationScheduleRepository scheduleRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

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
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @Autowired
    private com.calibratesys.repository.LaboratoryStaffRepository laboratoryStaffRepository;

    private String adminToken;
    private String staffToken;
    private Equipment defaultEquipment;
    private Equipment otherEquipment;
    private Laboratory defaultLab;
    private CalibrationSchedule defaultSchedule;

    @BeforeEach
    void setUp() {
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        Staff staff = staffRepository.findByEmail("rec.staff@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-REC-01", "Alice", "Walker", "rec.staff@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0166",
                        "Lab Assistant", "Biology", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff));

        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-REC-" + unique, "Record College " + unique, null, null, null));
        defaultLab = laboratoryRepository.save(new Laboratory(col, "L-REC-" + unique, "Default Lab " + unique, "Biology", "Room 301", LabStatus.ACTIVE));
        laboratoryStaffRepository.save(new com.calibratesys.entity.LaboratoryStaff(defaultLab, staff, com.calibratesys.entity.enums.StaffLabRole.LAB_TECHNICIAN, false));


        defaultEquipment = equipmentRepository.save(new Equipment(
                defaultLab, "AST-REC-1-" + unique, "SN-REC-1-" + unique, "Autoclave Sterilizer",
                "AS-500", "Tuttnauer", LocalDate.now(clock).minusMonths(12), LocalDate.now(clock).minusMonths(12),
                180, EquipmentStatus.CALIBRATION_DUE
        ));

        otherEquipment = equipmentRepository.save(new Equipment(
                defaultLab, "AST-REC-2-" + unique, "SN-REC-2-" + unique, "Incubator Shaker",
                "IS-200", "Innova", LocalDate.now(clock).minusMonths(8), LocalDate.now(clock).minusMonths(8),
                90, EquipmentStatus.OPERATIONAL
        ));

        defaultSchedule = scheduleRepository.save(new CalibrationSchedule(
                defaultEquipment, LocalDate.now(clock).minusDays(2), LocalDate.now(clock).plusDays(5),
                ScheduleStatus.UPCOMING, SchedulePriority.HIGH, "Annual autoclave inspection"
        ));
    }

    @Test
    @DisplayName("1. ADMIN can create calibration record linked to schedule -> 201 Created & schedule marked COMPLETED")
    void testAdminCanCreateRecordWithSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto(
                defaultEquipment.getId(),
                defaultSchedule.getId(),
                today,
                today.plusDays(180),
                CalibratedByType.INTERNAL,
                "Dr. Sarah Jenkins",
                CalibrationResult.PASSED,
                "+/- 0.05 C",
                "ISO/IEC 17025 Standard RTD",
                "Temperature and pressure verification successful"
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.equipmentId").value(defaultEquipment.getId()))
                .andExpect(jsonPath("$.scheduleId").value(defaultSchedule.getId()))
                .andExpect(jsonPath("$.result").value("PASSED"))
                .andExpect(jsonPath("$.calibratedByType").value("INTERNAL"))
                .andExpect(jsonPath("$.calibratorName").value("Dr. Sarah Jenkins"))
                .andExpect(jsonPath("$.assetTag").value(defaultEquipment.getAssetTag()))
                .andExpect(jsonPath("$.laboratoryId").value(defaultLab.getId()));

        // Verify schedule was marked COMPLETED
        CalibrationSchedule updatedSchedule = scheduleRepository.findById(defaultSchedule.getId()).orElseThrow();
        assertEquals(ScheduleStatus.COMPLETED, updatedSchedule.getStatus());

        // Verify equipment status synchronized back to OPERATIONAL on PASSED
        Equipment updatedEquip = equipmentRepository.findById(defaultEquipment.getId()).orElseThrow();
        assertEquals(EquipmentStatus.OPERATIONAL, updatedEquip.getStatus());
    }

    @Test
    @DisplayName("2. ADMIN can create standalone calibration record without schedule -> 201 Created")
    void testAdminCanCreateRecordWithoutSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto(
                defaultEquipment.getId(),
                null,
                today,
                today.plusDays(90),
                CalibratedByType.EXTERNAL_VENDOR,
                "Metrology Labs International",
                CalibrationResult.PASSED_WITH_CONDITIONS,
                "+/- 0.1 C",
                "NIST Traceable Weights",
                "Minor drift noted on probe 2"
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.scheduleId").doesNotExist())
                .andExpect(jsonPath("$.result").value("PASSED_WITH_CONDITIONS"))
                .andExpect(jsonPath("$.calibratedByType").value("EXTERNAL_VENDOR"));
    }

    @Test
    @DisplayName("3. Creation with nonexistent equipment returns 404 Not Found")
    void testCreateRecordNonexistentEquipment() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto(
                999999L,
                null,
                today,
                today.plusDays(90),
                CalibratedByType.INTERNAL,
                "Tech A",
                CalibrationResult.PASSED,
                null, null, null
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Equipment not found")));
    }

    @Test
    @DisplayName("4. Creation with nonexistent schedule returns 404 Not Found")
    void testCreateRecordNonexistentSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto(
                defaultEquipment.getId(),
                999999L,
                today,
                today.plusDays(90),
                CalibratedByType.INTERNAL,
                "Tech A",
                CalibrationResult.PASSED,
                null, null, null
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Calibration schedule not found")));
    }

    @Test
    @DisplayName("5. Mismatched equipment and schedule returns 400 Bad Request")
    void testCreateRecordMismatchedEquipmentAndSchedule() throws Exception {
        LocalDate today = LocalDate.now(clock);
        // defaultSchedule belongs to defaultEquipment, but we pass otherEquipment.getId()
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto(
                otherEquipment.getId(),
                defaultSchedule.getId(),
                today,
                today.plusDays(90),
                CalibratedByType.INTERNAL,
                "Tech A",
                CalibrationResult.PASSED,
                null, null, null
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("belongs to equipment")));
    }

    @Test
    @DisplayName("6. Next due date before calibration date returns 400 Bad Request")
    void testCreateRecordNextDueDateBeforeCalibrationDate() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto(
                defaultEquipment.getId(),
                null,
                today,
                today.minusDays(1),
                CalibratedByType.INTERNAL,
                "Tech A",
                CalibrationResult.PASSED,
                null, null, null
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Next due date cannot be before calibration date")));
    }

    @Test
    @DisplayName("7. Missing required fields returns 400 Bad Request")
    void testCreateRecordMissingRequiredFields() throws Exception {
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto();

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8. Both ADMIN and LAB_STAFF can retrieve records and single record by ID")
    void testRecordRetrievalAdminAndStaff() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecord record = recordRepository.save(new CalibrationRecord(
                defaultEquipment, null, today, today.plusDays(90),
                CalibratedByType.INTERNAL, "Inspector Gadget", CalibrationResult.PASSED,
                "+/- 0.01", "Standard A", "Notes"
        ));

        // ADMIN access
        mockMvc.perform(get("/api/calibration-records/" + record.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId()))
                .andExpect(jsonPath("$.equipmentId").value(defaultEquipment.getId()));

        // LAB_STAFF access
        mockMvc.perform(get("/api/calibration-records/" + record.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId()))
                .andExpect(jsonPath("$.equipmentId").value(defaultEquipment.getId()));
    }

    @Test
    @DisplayName("9. Equipment calibration history returns records sorted by calibrationDate DESC")
    void testEquipmentCalibrationHistory() throws Exception {
        LocalDate today = LocalDate.now(clock);
        recordRepository.save(new CalibrationRecord(
                defaultEquipment, null, today.minusMonths(6), today,
                CalibratedByType.INTERNAL, "Old Tech", CalibrationResult.PASSED,
                null, null, "Older Record"
        ));
        recordRepository.save(new CalibrationRecord(
                defaultEquipment, null, today, today.plusMonths(6),
                CalibratedByType.INTERNAL, "New Tech", CalibrationResult.PASSED,
                null, null, "Newer Record"
        ));

        mockMvc.perform(get("/api/equipment/" + defaultEquipment.getId() + "/calibration-records")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].calibrationDate").value(today.toString()))
                .andExpect(jsonPath("$[1].calibrationDate").value(today.minusMonths(6).toString()));

        // Nonexistent equipment history -> 404
        mockMvc.perform(get("/api/equipment/999999/calibration-records")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. Schedule calibration history endpoint returns records associated with that schedule")
    void testScheduleCalibrationHistory() throws Exception {
        LocalDate today = LocalDate.now(clock);
        recordRepository.save(new CalibrationRecord(
                defaultEquipment, defaultSchedule, today, today.plusDays(90),
                CalibratedByType.INTERNAL, "Calibrator X", CalibrationResult.PASSED,
                null, null, "Schedule link test"
        ));

        mockMvc.perform(get("/api/calibration-schedules/" + defaultSchedule.getId() + "/calibration-records")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].scheduleId").value(defaultSchedule.getId()));

        // Nonexistent schedule history -> 404
        mockMvc.perform(get("/api/calibration-schedules/999999/calibration-records")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("11. Filtering calibration records by equipment, laboratory, result, calibratedByType, and date range")
    void testFilteringCalibrationRecords() throws Exception {
        LocalDate today = LocalDate.now(clock);
        recordRepository.save(new CalibrationRecord(
                defaultEquipment, null, today.minusDays(5), today.plusDays(90),
                CalibratedByType.INTERNAL, "Internal Tech", CalibrationResult.FAILED,
                null, null, "Failed Record"
        ));
        recordRepository.save(new CalibrationRecord(
                defaultEquipment, null, today, today.plusDays(180),
                CalibratedByType.EXTERNAL_VENDOR, "External Tech", CalibrationResult.PASSED,
                null, null, "Passed Record"
        ));

        // Filter by equipment and result FAILED
        mockMvc.perform(get("/api/calibration-records?equipmentId=" + defaultEquipment.getId() + "&result=FAILED")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].result").value("FAILED"));

        // Filter by equipment and calibratedByType EXTERNAL_VENDOR
        mockMvc.perform(get("/api/calibration-records?equipmentId=" + defaultEquipment.getId() + "&calibratedByType=EXTERNAL_VENDOR")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].calibratedByType").value("EXTERNAL_VENDOR"));

        // Filter by laboratoryId
        mockMvc.perform(get("/api/calibration-records?laboratoryId=" + defaultLab.getId())
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Filter by date range
        mockMvc.perform(get("/api/calibration-records?equipmentId=" + defaultEquipment.getId() + "&fromDate=" + today.minusDays(2) + "&toDate=" + today.plusDays(2))
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].result").value("PASSED"));
    }

    @Test
    @DisplayName("12. History preservation: calibration records do not have a DELETE endpoint")
    void testNoDeleteEndpointForCalibrationRecords() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecord record = recordRepository.save(new CalibrationRecord(
                defaultEquipment, null, today, today.plusDays(90),
                CalibratedByType.INTERNAL, "Tech B", CalibrationResult.PASSED,
                null, null, "Immutable record"
        ));

        // Attempt DELETE -> 405 Method Not Allowed
        mockMvc.perform(delete("/api/calibration-records/" + record.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("13. LAB_STAFF receives 403 Forbidden when attempting to create calibration records")
    void testLabStaffForbiddenOnCreate() throws Exception {
        LocalDate today = LocalDate.now(clock);
        CalibrationRecordRequestDto request = new CalibrationRecordRequestDto(
                defaultEquipment.getId(), null, today, today.plusDays(90),
                CalibratedByType.INTERNAL, "Staff Tech", CalibrationResult.PASSED,
                null, null, null
        );

        mockMvc.perform(post("/api/calibration-records")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. Unauthenticated requests to calibration records endpoints receive 401 Unauthorized")
    void testUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/calibration-records"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/calibration-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
