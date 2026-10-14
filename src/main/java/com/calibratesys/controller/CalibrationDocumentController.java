package com.calibratesys.controller;

import com.calibratesys.dto.CalibrationDocumentResponseDto;
import com.calibratesys.dto.DownloadedDocumentDto;
import com.calibratesys.service.CalibrationDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Calibration Certificates and Document Management.
 * Upload and deletion are restricted to ADMIN role.
 * Document viewing and downloads are accessible to both ADMIN and LAB_STAFF.
 */
@RestController
@RequestMapping("/api/calibration-documents")
public class CalibrationDocumentController {

    private final CalibrationDocumentService documentService;

    @Autowired
    public CalibrationDocumentController(CalibrationDocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Upload a new calibration certificate or equipment document (ADMIN only).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CalibrationDocumentResponseDto> uploadDocument(
            @RequestParam("equipmentId") Long equipmentId,
            @RequestParam(value = "calibrationRecordId", required = false) Long calibrationRecordId,
            @RequestParam("documentName") String documentName,
            @RequestParam(value = "certificateNumber", required = false) String certificateNumber,
            @RequestParam(value = "issueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(value = "validUntil", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
            @RequestParam("file") MultipartFile file) {
        CalibrationDocumentResponseDto created = documentService.uploadDocument(
                equipmentId, calibrationRecordId, documentName, certificateNumber, issueDate, validUntil, file
        );
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Retrieve a list of calibration documents with optional filtering.
     */
    @GetMapping
    public ResponseEntity<?> getDocuments(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) Long calibrationRecordId,
            @RequestParam(required = false) String certificateNumber,
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String validity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueEndDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validEndDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            com.calibratesys.util.PaginationValidator.validate(p, s);
            com.calibratesys.dto.PageResponseDto<CalibrationDocumentResponseDto> result = documentService.searchDocuments(
                    equipmentId, laboratoryId, calibrationRecordId,
                    certificateNumber, documentName, fileType, validity,
                    issueStartDate, issueEndDate, validStartDate, validEndDate,
                    search, sortBy, sortDirection, p, s
            );
            return ResponseEntity.ok(result);
        }
        List<CalibrationDocumentResponseDto> documents = documentService.getAllDocumentsWithFilters(
                equipmentId, calibrationRecordId, certificateNumber,
                issueStartDate, issueEndDate, validStartDate, validEndDate
        );
        return ResponseEntity.ok(documents);
    }

    /**
     * Retrieve document metadata by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CalibrationDocumentResponseDto> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    /**
     * Download the physical document file securely.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        DownloadedDocumentDto downloaded = documentService.downloadDocument(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloaded.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloaded.getFilename() + "\"")
                .contentLength(downloaded.getContentLength())
                .body(downloaded.getResource());
    }

    /**
     * Delete an unassociated equipment document (ADMIN only).
     * Documents linked to calibration records are protected for audit compliance and cannot be deleted.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
