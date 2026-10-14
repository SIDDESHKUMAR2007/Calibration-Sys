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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CalibrationDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalibrationDocumentRepository documentRepository;

    @Autowired
    private CalibrationRecordRepository recordRepository;

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
    private com.calibratesys.repository.LaboratoryStaffRepository laboratoryStaffRepository;

    private String adminToken;
    private String staffToken;
    private Equipment defaultEquipment;
    private Equipment otherEquipment;
    private CalibrationRecord defaultRecord;

    @BeforeEach
    void setUp() {
        Staff admin = staffRepository.findByEmail("admin@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "ADMIN-001", "System", "Admin", "admin@calibratesys.edu",
                        passwordEncoder.encode("Admin@12345"), "555-0199",
                        "System Administrator", "Administration", SystemRole.ADMIN, StaffStatus.ACTIVE
                )));
        adminToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(admin));

        Staff staff = staffRepository.findByEmail("doc.staff@calibratesys.edu")
                .orElseGet(() -> staffRepository.save(new Staff(
                        "STF-DOC-01", "Bob", "Taylor", "doc.staff@calibratesys.edu",
                        passwordEncoder.encode("Secret@123"), "555-0188",
                        "Lab Assistant", "Chemistry", SystemRole.LAB_STAFF, StaffStatus.ACTIVE
                )));
        staffToken = tokenProvider.generateTokenFromUserDetails(new CustomUserDetails(staff));

        String unique = String.valueOf(System.currentTimeMillis()).substring(7);
        College col = collegeRepository.save(new College("C-DOC-" + unique, "Document College " + unique, null, null, null));
        Laboratory defaultLab = laboratoryRepository.save(new Laboratory(col, "L-DOC-" + unique, "Document Lab " + unique, "Chemistry", "Room 402", LabStatus.ACTIVE));
        laboratoryStaffRepository.save(new com.calibratesys.entity.LaboratoryStaff(defaultLab, staff, com.calibratesys.entity.enums.StaffLabRole.LAB_TECHNICIAN, false));


        defaultEquipment = equipmentRepository.save(new Equipment(
                defaultLab, "AST-DOC-1-" + unique, "SN-DOC-1-" + unique, "Spectrophotometer",
                "SPEC-100", "Shimadzu", LocalDate.now().minusMonths(6), LocalDate.now().minusMonths(6),
                365, EquipmentStatus.OPERATIONAL
        ));

        otherEquipment = equipmentRepository.save(new Equipment(
                defaultLab, "AST-DOC-2-" + unique, "SN-DOC-2-" + unique, "Centrifuge",
                "CF-50", "Eppendorf", LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(3),
                180, EquipmentStatus.OPERATIONAL
        ));

        defaultRecord = recordRepository.save(new CalibrationRecord(
                defaultEquipment, null, LocalDate.now().minusDays(10), LocalDate.now().plusMonths(6),
                CalibratedByType.INTERNAL, "Dr. Calibrator", CalibrationResult.PASSED, "0.01", "ISO-17025", "Standard check"
        ));
    }

    private MockMultipartFile createValidPdf(String filename) {
        byte[] pdfBytes = "%PDF-1.5 Sample Certificate Content".getBytes(StandardCharsets.UTF_8);
        return new MockMultipartFile("file", filename, "application/pdf", pdfBytes);
    }

    private MockMultipartFile createValidPng(String filename) {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
        return new MockMultipartFile("file", filename, "image/png", pngHeader);
    }

    @Test
    @DisplayName("Upload document as ADMIN - Success with all fields")
    void uploadDocument_AsAdmin_Success() throws Exception {
        MockMultipartFile file = createValidPdf("cert-001.pdf");

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("calibrationRecordId", defaultRecord.getId().toString())
                        .param("documentName", "Calibration Certificate 2026")
                        .param("certificateNumber", "CERT-2026-SPEC-01")
                        .param("issueDate", LocalDate.now().minusDays(5).toString())
                        .param("validUntil", LocalDate.now().plusMonths(12).toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.equipmentId").value(defaultEquipment.getId()))
                .andExpect(jsonPath("$.calibrationRecordId").value(defaultRecord.getId()))
                .andExpect(jsonPath("$.documentName").value("Calibration Certificate 2026"))
                .andExpect(jsonPath("$.certificateNumber").value("CERT-2026-SPEC-01"))
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.validityStatus").value("VALID"));
    }

    @Test
    @DisplayName("Upload valid PNG image document as ADMIN - Success")
    void uploadDocument_ValidPng_Success() throws Exception {
        MockMultipartFile file = createValidPng("cert-image.png");

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Certificate Sticker Photo")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType").value("image/png"))
                .andExpect(jsonPath("$.validityStatus").value("NO_EXPIRY"));
    }

    @Test
    @DisplayName("Upload document as LAB_STAFF - Forbidden 403")
    void uploadDocument_AsLabStaff_Forbidden() throws Exception {
        MockMultipartFile file = createValidPdf("cert.pdf");

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Cert")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Upload document unauthenticated - Unauthorized 401")
    void uploadDocument_Unauthenticated_Unauthorized() throws Exception {
        MockMultipartFile file = createValidPdf("cert.pdf");

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Cert"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Upload document with invalid PDF magic bytes signature - Returns 400 Bad Request")
    void uploadDocument_InvalidPdfSignature_BadRequest() throws Exception {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "NOT_A_REAL_PDF_HEADER".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(fakePdf)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Corrupt File")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid file content")));
    }

    @Test
    @DisplayName("Upload document with unsupported extension (.exe) - Returns 400 Bad Request")
    void uploadDocument_UnsupportedExtension_BadRequest() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", "MZbinarycode".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(exeFile)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Malware")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported file extension")));
    }

    @Test
    @DisplayName("Upload document with path traversal filename - Returns 400 Bad Request")
    void uploadDocument_PathTraversal_BadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../traversal.pdf", "application/pdf", "%PDF-1.4 test content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Traversal")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("path traversal")));
    }

    @Test
    @DisplayName("Upload document with validUntil before issueDate - Returns 400 Bad Request")
    void uploadDocument_InvalidDates_BadRequest() throws Exception {
        MockMultipartFile file = createValidPdf("cert.pdf");

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Date Error")
                        .param("issueDate", "2026-06-01")
                        .param("validUntil", "2026-05-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Validity end date cannot be before issue date")));
    }

    @Test
    @DisplayName("Upload document for nonexistent equipment - Returns 404 Not Found")
    void uploadDocument_NonexistentEquipment_NotFound() throws Exception {
        MockMultipartFile file = createValidPdf("cert.pdf");

        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", "999999")
                        .param("documentName", "Cert")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Equipment not found")));
    }

    @Test
    @DisplayName("Upload document with mismatched calibration record and equipment - Returns 400 Bad Request")
    void uploadDocument_MismatchedRecordEquipment_BadRequest() throws Exception {
        MockMultipartFile file = createValidPdf("cert.pdf");

        // defaultRecord belongs to defaultEquipment, but we pass otherEquipment.getId()
        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", otherEquipment.getId().toString())
                        .param("calibrationRecordId", defaultRecord.getId().toString())
                        .param("documentName", "Mismatch Test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to the specified equipment")));
    }

    @Test
    @DisplayName("Get document metadata by ID - Success for both ADMIN and LAB_STAFF")
    void getDocumentById_Success() throws Exception {
        MockMultipartFile file = createValidPdf("cert-retrieve.pdf");
        String responseContent = mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Retrievable Document")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long docId = ((Number) com.jayway.jsonpath.JsonPath.read(responseContent, "$.id")).longValue();

        // LAB_STAFF should be able to view metadata
        mockMvc.perform(get("/api/calibration-documents/" + docId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.documentName").value("Retrievable Document"));
    }

    @Test
    @DisplayName("Download document file - Success for LAB_STAFF")
    void downloadDocument_Success() throws Exception {
        MockMultipartFile file = createValidPdf("cert-download.pdf");
        String responseContent = mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Downloadable Cert")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long docId = ((Number) com.jayway.jsonpath.JsonPath.read(responseContent, "$.id")).longValue();

        mockMvc.perform(get("/api/calibration-documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=")))
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @DisplayName("Download nonexistent document - Returns 404 Not Found")
    void downloadDocument_NotFound() throws Exception {
        mockMvc.perform(get("/api/calibration-documents/999999/download")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get documents by equipment history - Success")
    void getDocumentsByEquipment_Success() throws Exception {
        MockMultipartFile file = createValidPdf("cert-eq.pdf");
        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Equipment Doc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/equipment/" + defaultEquipment.getId() + "/calibration-documents")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Get documents by calibration record history - Success")
    void getDocumentsByCalibrationRecord_Success() throws Exception {
        MockMultipartFile file = createValidPdf("cert-rec.pdf");
        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("calibrationRecordId", defaultRecord.getId().toString())
                        .param("documentName", "Record Doc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/calibration-records/" + defaultRecord.getId() + "/documents")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Delete document associated with calibration record - Rejected 400 Bad Request")
    void deleteDocument_AssociatedWithRecord_Rejected() throws Exception {
        MockMultipartFile file = createValidPdf("cert-immutable.pdf");
        String responseContent = mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("calibrationRecordId", defaultRecord.getId().toString())
                        .param("documentName", "Immutable Cert")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long docId = ((Number) com.jayway.jsonpath.JsonPath.read(responseContent, "$.id")).longValue();

        mockMvc.perform(delete("/api/calibration-documents/" + docId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot delete document associated with a calibration record")));
    }

    @Test
    @DisplayName("Delete unassociated document by ADMIN - Success 204 No Content")
    void deleteDocument_Unassociated_Success() throws Exception {
        MockMultipartFile file = createValidPdf("temp-manual.pdf");
        String responseContent = mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Temporary Manual")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long docId = ((Number) com.jayway.jsonpath.JsonPath.read(responseContent, "$.id")).longValue();

        mockMvc.perform(delete("/api/calibration-documents/" + docId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(documentRepository.existsById(docId));
    }

    @Test
    @DisplayName("Delete document by LAB_STAFF - Forbidden 403")
    void deleteDocument_AsLabStaff_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/calibration-documents/1")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Filter documents by certificateNumber and dates - Success")
    void filterDocuments_Success() throws Exception {
        MockMultipartFile file = createValidPdf("cert-filtered.pdf");
        mockMvc.perform(multipart("/api/calibration-documents")
                        .file(file)
                        .param("equipmentId", defaultEquipment.getId().toString())
                        .param("documentName", "Filtered Cert")
                        .param("certificateNumber", "CERT-FILTER-99")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/calibration-documents")
                        .param("certificateNumber", "FILTER-99")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
