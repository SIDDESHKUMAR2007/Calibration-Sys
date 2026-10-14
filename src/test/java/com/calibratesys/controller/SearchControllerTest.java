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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest {

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

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        adminStaff = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-SCH-01", "Global", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0101",
                        "System Admin", "IT", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(adminStaff));

        scopedStaff = staffRepository.save(new Staff(
                "STF-SCH-" + unique, "Search", "User" + unique, "search.staff." + unique + "@calibratesys.edu",
                passwordEncoder.encode("Secret@123"), "555-0102",
                "Lab Tech", "Biology", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
        ));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(scopedStaff));

        College college = collegeRepository.save(new College("COL-SCH-" + unique, "Search College " + unique, null, null, null));
        labA = laboratoryRepository.save(new Laboratory(college, "LAB-A-" + unique, "Alpha Lab " + unique, "Biology", "Room A", LabStatus.ACTIVE));
        labB = laboratoryRepository.save(new Laboratory(college, "LAB-B-" + unique, "Beta Lab " + unique, "Biology", "Room B", LabStatus.ACTIVE));
        labC = laboratoryRepository.save(new Laboratory(college, "LAB-C-" + unique, "Gamma Lab " + unique, "Chemistry", "Room C", LabStatus.ACTIVE));

        // Assign scopedStaff to Lab A and Lab B ONLY
        laboratoryStaffRepository.save(new LaboratoryStaff(labA, scopedStaff, StaffLabRole.PRIMARY_INCHARGE, true));
        laboratoryStaffRepository.save(new LaboratoryStaff(labB, scopedStaff, StaffLabRole.LAB_TECHNICIAN, false));
    }

    @Test
    @DisplayName("1. Blank search query returns 400 Bad Request")
    void testBlankQueryReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be blank")));

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("2. Search query exceeding 200 characters returns 400 Bad Request")
    void testQueryExceeding200CharsReturnsBadRequest() throws Exception {
        String longQuery = "a".repeat(201);
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", longQuery))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot exceed 200 characters")));
    }

    @Test
    @DisplayName("3. Mandatory Multi-Laboratory Data Scoping: Staff only sees assigned labs A & B; Lab C omitted")
    void testMandatoryMultiLabDataScoping() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        Equipment eqA = equipmentRepository.save(new Equipment(
                labA, "AST-A-" + unique, "SN-A-" + unique, "Spectroscope " + unique + " Alpha",
                "Model-A", "Vendor-X", LocalDate.now().minusDays(50), LocalDate.now().minusDays(40), 90, EquipmentStatus.OPERATIONAL
        ));
        Equipment eqB = equipmentRepository.save(new Equipment(
                labB, "AST-B-" + unique, "SN-B-" + unique, "Spectroscope " + unique + " Beta",
                "Model-B", "Vendor-X", LocalDate.now().minusDays(60), LocalDate.now().minusDays(50), 90, EquipmentStatus.OPERATIONAL
        ));
        Equipment eqC = equipmentRepository.save(new Equipment(
                labC, "AST-C-" + unique, "SN-C-" + unique, "Spectroscope " + unique + " Gamma",
                "Model-C", "Vendor-X", LocalDate.now().minusDays(70), LocalDate.now().minusDays(60), 90, EquipmentStatus.OPERATIONAL
        ));

        // As LAB_STAFF: global search for "Spectroscope " + unique
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("q", "Spectroscope " + unique))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment[*].id", hasItems(eqA.getId().intValue(), eqB.getId().intValue())))
                .andExpect(jsonPath("$.equipment[*].id", not(hasItem(eqC.getId().intValue()))));

        // As ADMIN: global search for "Spectroscope " + unique includes all three
        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", "Spectroscope " + unique))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment[*].id", hasItems(eqA.getId().intValue(), eqB.getId().intValue(), eqC.getId().intValue())));

        // LAB_STAFF requesting Lab C explicitly -> 403 Forbidden
        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("laboratoryId", String.valueOf(labC.getId()))
                        .param("page", "0"))
                .andExpect(status().isForbidden());

        // ADMIN requesting Lab C explicitly -> 200 OK
        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("laboratoryId", String.valueOf(labC.getId()))
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("4. Equipment Advanced Search, Sorting, and Pagination")
    void testEquipmentAdvancedSearchSortingPagination() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        Equipment eq = equipmentRepository.save(new Equipment(
                labA, "AST-ADV-" + unique, "SN-ADV-" + unique, "Advanced Centrifuge " + unique,
                "AC-500", "Eppendorf", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5), 180, EquipmentStatus.OPERATIONAL
        ));

        // Sort ASC
        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Advanced Centrifuge " + unique)
                        .param("sortBy", "name")
                        .param("sortDirection", "ASC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Advanced Centrifuge " + unique))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));

        // Invalid sort field -> 400 Bad Request
        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("sortBy", "malicious_column")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort field")));

        // Invalid sort direction -> 400 Bad Request
        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("sortDirection", "SIDEWAYS")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort direction")));
    }

    @Test
    @DisplayName("5. Strict date validation: startDate > endDate returns 400 Bad Request")
    void testDateRangeValidation() throws Exception {
        // Equipment purchase dates: from > to
        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("purchaseStartDate", "2026-12-31")
                        .param("purchaseEndDate", "2026-01-01")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Purchase start date cannot be after end date")));

        // Schedule due dates: from > to
        mockMvc.perform(get("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("dueStartDate", "2026-10-30")
                        .param("dueEndDate", "2026-10-01")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Due start date cannot be after end date")));

        // Record calibration dates: from > to
        mockMvc.perform(get("/api/calibration-records")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("calibrationStartDate", "2026-10-30")
                        .param("calibrationEndDate", "2026-10-01")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Calibration start date cannot be after end date")));

        // Document issue dates: from > to
        mockMvc.perform(get("/api/calibration-documents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("issueStartDate", "2026-10-30")
                        .param("issueEndDate", "2026-10-01")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Issue start date cannot be after end date")));
    }

    @Test
    @DisplayName("6. Calibration Document Validity Filtering and Validation")
    void testDocumentValidityFiltering() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis()).substring(7);

        Equipment eq = equipmentRepository.save(new Equipment(
                labA, "AST-DOC-" + unique, "SN-DOC-" + unique, "Doc Balance " + unique,
                "DB-1", "Vendor", LocalDate.now(), LocalDate.now(), 90, EquipmentStatus.OPERATIONAL
        ));

        // Create expired document
        CalibrationDocument expiredDoc = new CalibrationDocument(
                eq, null, "Expired Certificate " + unique, "CERT-EXP-" + unique,
                "dummy/path1", "application/pdf", 1024L,
                LocalDate.now().minusDays(365), LocalDate.now().minusDays(10)
        );
        calibrationDocumentRepository.save(expiredDoc);

        // Create valid document
        CalibrationDocument validDoc = new CalibrationDocument(
                eq, null, "Valid Certificate " + unique, "CERT-VAL-" + unique,
                "dummy/path2", "application/pdf", 1024L,
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(100)
        );
        calibrationDocumentRepository.save(validDoc);

        // Search EXPIRED
        mockMvc.perform(get("/api/calibration-documents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("validity", "EXPIRED")
                        .param("search", unique)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].certificateNumber", hasItem("CERT-EXP-" + unique)))
                .andExpect(jsonPath("$.content[*].certificateNumber", not(hasItem("CERT-VAL-" + unique))));

        // Search VALID
        mockMvc.perform(get("/api/calibration-documents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("validity", "VALID")
                        .param("search", unique)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].certificateNumber", hasItem("CERT-VAL-" + unique)))
                .andExpect(jsonPath("$.content[*].certificateNumber", not(hasItem("CERT-EXP-" + unique))));

        // Invalid validity filter -> 400 Bad Request
        mockMvc.perform(get("/api/calibration-documents")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("validity", "TOTALLY_INVALID")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid validity filter")));
    }

    @Test
    @DisplayName("7. Search operations are read-only and emit NO audit log entries")
    void testSearchDoesNotCreateAuditLogs() throws Exception {
        long initialAuditCount = auditLogRepository.count();

        mockMvc.perform(get("/api/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("q", "alpha"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/equipment")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "alpha")
                        .param("page", "0"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/calibration-schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/laboratories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "alpha")
                        .param("page", "0"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "alpha")
                        .param("page", "0"))
                .andExpect(status().isOk());

        long afterAuditCount = auditLogRepository.count();
        assertEquals(initialAuditCount, afterAuditCount, "Search operations must be strictly read-only and generate zero audit events.");
    }
}
