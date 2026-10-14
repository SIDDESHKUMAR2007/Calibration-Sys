package com.calibratesys.controller;

import com.calibratesys.dto.CollegeRequestDto;
import com.calibratesys.dto.LoginRequestDto;
import com.calibratesys.dto.StaffRequestDto;
import com.calibratesys.entity.AuditLog;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.AuditLogRepository;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.security.JwtTokenProvider;
import com.calibratesys.service.CollegeService;
import com.calibratesys.service.StaffService;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollegeService collegeService;

    @Autowired
    private StaffService staffService;

    private String adminToken;
    private String labStaffToken;
    private Staff adminStaff;
    private Staff labStaff;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        adminStaff = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(adminStaff));

        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        labStaff = staffRepository.save(new Staff(
                "STF-AUD-" + unique, "Auditor", "Staff", "staff.aud." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0789",
                "Laboratory Tech", "Chemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        labStaffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(labStaff));
    }

    @Test
    @DisplayName("Admin can retrieve paginated audit logs")
    void getAuditLogs_AsAdmin_Success() throws Exception {
        AuditLog log1 = new AuditLog(adminStaff, AuditAction.COLLEGE_CREATED, AuditEntityType.COLLEGE, 101L,
                "College created", null, "{\"name\":\"Engineering\"}", "127.0.0.1");
        auditLogRepository.save(log1);

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].action", is("COLLEGE_CREATED")))
                .andExpect(jsonPath("$.content[0].entityType", is("COLLEGE")))
                .andExpect(jsonPath("$.content[0].actorStaffEmail", is(adminStaff.getEmail())));
    }

    @Test
    @DisplayName("LAB_STAFF role is forbidden from accessing audit logs")
    void getAuditLogs_AsLabStaff_Forbidden() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + labStaffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user receives 401 Unauthorized for audit logs")
    void getAuditLogs_Unauthenticated_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can get audit log by ID")
    void getAuditLogById_AsAdmin_Success() throws Exception {
        AuditLog log = auditLogRepository.save(new AuditLog(
                adminStaff, AuditAction.LABORATORY_CREATED, AuditEntityType.LABORATORY, 202L,
                "Laboratory created", null, "{\"name\":\"Bio Lab\"}", "127.0.0.1"
        ));

        mockMvc.perform(get("/api/audit-logs/" + log.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(log.getId().intValue())))
                .andExpect(jsonPath("$.action", is("LABORATORY_CREATED")))
                .andExpect(jsonPath("$.entityType", is("LABORATORY")))
                .andExpect(jsonPath("$.entityId", is(202)))
                .andExpect(jsonPath("$.description", is("Laboratory created")));
    }

    @Test
    @DisplayName("Audit log by non-existent ID returns 404")
    void getAuditLogById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/audit-logs/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Audit logs are immutable: PUT and DELETE endpoints return 405 Method Not Allowed")
    void auditLogs_AreImmutable_CannotModifyOrDelete() throws Exception {
        AuditLog log = auditLogRepository.save(new AuditLog(
                adminStaff, AuditAction.COLLEGE_DELETED, AuditEntityType.COLLEGE, 303L,
                "College deleted", null, null, "127.0.0.1"
        ));

        mockMvc.perform(put("/api/audit-logs/" + log.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tampered\"}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/audit-logs/" + log.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Filter audit logs by action, entityType, actorStaffId, and date range")
    void getAuditLogs_WithFilters_ReturnsFilteredResults() throws Exception {
        AuditLog log1 = auditLogRepository.save(new AuditLog(
                adminStaff, AuditAction.COLLEGE_CREATED, AuditEntityType.COLLEGE, 10L,
                "College 10 created", null, null, "127.0.0.1"
        ));
        AuditLog log2 = auditLogRepository.save(new AuditLog(
                labStaff, AuditAction.NOTIFICATION_MARKED_READ, AuditEntityType.NOTIFICATION, 50L,
                "Notification read", null, null, "127.0.0.1"
        ));

        // Filter by action
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("action", "COLLEGE_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].action", is("COLLEGE_CREATED")));

        // Filter by entityType
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("entityType", "NOTIFICATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].entityType", is("NOTIFICATION")));

        // Filter by actorStaffId
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("actorStaffId", labStaff.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].actorStaffId", is(labStaff.getId().intValue())));

        // Filter by date range (today)
        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", today.toString())
                        .param("toDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("Admin can retrieve audit history for a specific entity")
    void getEntityAuditLogs_ReturnsHistoryForEntity() throws Exception {
        auditLogRepository.save(new AuditLog(
                adminStaff, AuditAction.EQUIPMENT_CREATED, AuditEntityType.EQUIPMENT, 555L,
                "Equipment created", null, "{\"name\":\"Centrifuge\"}", "127.0.0.1"
        ));
        auditLogRepository.save(new AuditLog(
                adminStaff, AuditAction.EQUIPMENT_UPDATED, AuditEntityType.EQUIPMENT, 555L,
                "Equipment updated", "{\"status\":\"OPERATIONAL\"}", "{\"status\":\"UNDER_MAINTENANCE\"}", "127.0.0.1"
        ));
        auditLogRepository.save(new AuditLog(
                adminStaff, AuditAction.EQUIPMENT_CREATED, AuditEntityType.EQUIPMENT, 999L,
                "Other equipment created", null, null, "127.0.0.1"
        ));

        mockMvc.perform(get("/api/audit-logs/entity/EQUIPMENT/555")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].entityId", is(555)))
                .andExpect(jsonPath("$.content[1].entityId", is(555)));
    }

    @Test
    @DisplayName("Admin can retrieve audit history for a specific actor")
    void getActorAuditLogs_ReturnsHistoryForActor() throws Exception {
        auditLogRepository.save(new AuditLog(
                labStaff, AuditAction.NOTIFICATION_MARKED_READ, AuditEntityType.NOTIFICATION, 10L,
                "Read notif 1", null, null, "127.0.0.1"
        ));
        auditLogRepository.save(new AuditLog(
                labStaff, AuditAction.NOTIFICATION_MARKED_READ, AuditEntityType.NOTIFICATION, 20L,
                "Read notif 2", null, null, "127.0.0.1"
        ));

        mockMvc.perform(get("/api/audit-logs/actor/" + labStaff.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].actorStaffId", is(labStaff.getId().intValue())))
                .andExpect(jsonPath("$.content[1].actorStaffId", is(labStaff.getId().intValue())));
    }

    @Test
    @DisplayName("Sensitive data (passwords, tokens) are never recorded in audit log diffs")
    void sensitiveData_NeverStoredInAuditLogs() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        StaffRequestDto createDto = new StaffRequestDto();
        createDto.setStaffId("STF-SEC-" + unique);
        createDto.setFirstName("Security");
        createDto.setLastName("Test");
        createDto.setEmail("sec.test." + unique + "@calibratesys.edu");
        createDto.setPassword("PlainSecretPassword123!");
        createDto.setPhoneNumber("555-9988");
        createDto.setDesignation("Auditor");
        createDto.setDepartment("Security");
        createDto.setSystemRole("LAB_STAFF");
        createDto.setStatus("ACTIVE");

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated());

        // Check audit log for STAFF_CREATED
        AuditLog auditLog = auditLogRepository.findAll().stream()
                .filter(l -> l.getAction() == AuditAction.STAFF_CREATED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected STAFF_CREATED audit log"));

        String newValues = auditLog.getNewValues();
        assertFalse(newValues.contains("PlainSecretPassword123!"), "Audit log must not contain plaintext password");
        assertFalse(newValues.contains("passwordHash"), "Audit log must not contain password hash field");
    }

    @Test
    @DisplayName("Failed login attempts are audited with failure action and no password leakage")
    void failedLogin_AuditedSafely() throws Exception {
        LoginRequestDto badLogin = new LoginRequestDto("admin@calibratesys.edu", "WrongPassword!999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        AuditLog failureLog = auditLogRepository.findAll().stream()
                .filter(l -> l.getAction() == AuditAction.LOGIN_FAILURE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected LOGIN_FAILURE audit log"));

        assertTrue(failureLog.getDescription().contains("Failed login attempt"));
        assertFalse(failureLog.getDescription().contains("WrongPassword!999"), "Password must not be in description");
        if (failureLog.getNewValues() != null) {
            assertFalse(failureLog.getNewValues().contains("WrongPassword!999"), "Password must not be in newValues");
        }
    }
}
