package com.calibratesys.service.impl;

import com.calibratesys.dto.CalibrationDocumentResponseDto;
import com.calibratesys.dto.DownloadedDocumentDto;
import com.calibratesys.entity.CalibrationDocument;
import com.calibratesys.entity.CalibrationRecord;
import com.calibratesys.entity.Equipment;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.CalibrationDocumentRepository;
import com.calibratesys.repository.CalibrationRecordRepository;
import com.calibratesys.repository.EquipmentRepository;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import com.calibratesys.service.AuditService;
import com.calibratesys.service.CalibrationDocumentService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CalibrationDocumentServiceImpl implements CalibrationDocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".png", ".jpg", ".jpeg");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("application/pdf", "image/png", "image/jpeg");

    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final CalibrationDocumentRepository calibrationDocumentRepository;
    private final EquipmentRepository equipmentRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;
    private final AuditService auditService;
    private final com.calibratesys.security.DataScopeService dataScopeService;
    private final java.time.Clock clock;

    @Value("${app.documents.storage-path:uploads/calibration-documents}")
    private String storagePathStr;

    @Value("${app.documents.max-file-size:10485760}")
    private long maxFileSizeBytes;

    private Path storageLocation;

    @Autowired
    public CalibrationDocumentServiceImpl(CalibrationDocumentRepository calibrationDocumentRepository,
                                          EquipmentRepository equipmentRepository,
                                          CalibrationRecordRepository calibrationRecordRepository,
                                          AuditService auditService,
                                          com.calibratesys.security.DataScopeService dataScopeService,
                                          java.time.Clock clock) {
        this.calibrationDocumentRepository = calibrationDocumentRepository;
        this.equipmentRepository = equipmentRepository;
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.auditService = auditService;
        this.dataScopeService = dataScopeService;
        this.clock = clock;
    }

    @PostConstruct
    public void init() {
        this.storageLocation = Paths.get(storagePathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize calibration documents storage directory", e);
        }
    }

    @Override
    @Transactional
    public CalibrationDocumentResponseDto uploadDocument(Long equipmentId, Long calibrationRecordId,
                                                         String documentName, String certificateNumber,
                                                         LocalDate issueDate, LocalDate validUntil,
                                                         MultipartFile file) {
        if (equipmentId == null) {
            throw new ApiException("Equipment ID is required", HttpStatus.BAD_REQUEST);
        }

        if (!StringUtils.hasText(documentName)) {
            throw new ApiException("Document name is required", HttpStatus.BAD_REQUEST);
        }

        if (documentName.trim().length() > 255) {
            throw new ApiException("Document name must not exceed 255 characters", HttpStatus.BAD_REQUEST);
        }

        if (StringUtils.hasText(certificateNumber) && certificateNumber.trim().length() > 100) {
            throw new ApiException("Certificate number must not exceed 100 characters", HttpStatus.BAD_REQUEST);
        }

        if (issueDate != null && validUntil != null && validUntil.isBefore(issueDate)) {
            throw new ApiException("Validity end date cannot be before issue date", HttpStatus.BAD_REQUEST);
        }

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ApiException("Equipment not found with id: " + equipmentId, HttpStatus.NOT_FOUND));

        CalibrationRecord calibrationRecord = null;
        if (calibrationRecordId != null) {
            calibrationRecord = calibrationRecordRepository.findById(calibrationRecordId)
                    .orElseThrow(() -> new ApiException("Calibration record not found with id: " + calibrationRecordId, HttpStatus.NOT_FOUND));

            if (!calibrationRecord.getEquipment().getId().equals(equipment.getId())) {
                throw new ApiException(
                        "The provided calibration record does not belong to the specified equipment",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Validate File
        String extension = validateFile(file);

        // Generate safe unique filename
        String safeFilename = UUID.randomUUID().toString() + extension;
        Path targetPath = this.storageLocation.resolve(safeFilename).normalize();

        // Enforce path traversal protection
        if (!targetPath.getParent().equals(this.storageLocation)) {
            throw new ApiException("Invalid file path traversal detected", HttpStatus.BAD_REQUEST);
        }

        // Write file to filesystem
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException("Failed to store uploaded document file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Persist metadata to database with cleanup rollback if DB save fails
        try {
            CalibrationDocument document = new CalibrationDocument();
            document.setEquipment(equipment);
            document.setCalibrationRecord(calibrationRecord);
            document.setDocumentName(documentName.trim());
            document.setCertificateNumber(StringUtils.hasText(certificateNumber) ? certificateNumber.trim() : null);
            document.setFilePath(safeFilename);
            document.setFileType(resolveMimeType(extension, file.getContentType()));
            document.setFileSizeBytes(file.getSize());
            document.setIssueDate(issueDate);
            document.setValidUntil(validUntil);
            document.setUploadedAt(LocalDateTime.now());

            CalibrationDocument saved = calibrationDocumentRepository.save(document);

            auditService.logEvent(
                    AuditAction.CALIBRATION_DOCUMENT_UPLOADED,
                    AuditEntityType.CALIBRATION_DOCUMENT,
                    saved.getId(),
                    "Calibration document '" + saved.getDocumentName() + "' (ID: " + saved.getId() + ") was uploaded for equipment " + equipment.getAssetTag() + ".",
                    null,
                    Map.of(
                            "documentId", saved.getId(),
                            "documentName", saved.getDocumentName(),
                            "equipmentId", equipment.getId(),
                            "calibrationRecordId", calibrationRecord != null ? calibrationRecord.getId() : "",
                            "fileSizeBytes", saved.getFileSizeBytes(),
                            "fileType", saved.getFileType(),
                            "certificateNumber", saved.getCertificateNumber() != null ? saved.getCertificateNumber() : ""
                    )
            );

            return mapToDto(saved);
        } catch (Exception ex) {
            // Cleanup orphaned physical file
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CalibrationDocumentResponseDto getDocumentById(Long id) {
        CalibrationDocument document = calibrationDocumentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration document not found with id: " + id, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = document.getEquipment() != null && document.getEquipment().getLaboratory() != null
                ? document.getEquipment().getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access calibration document in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        return mapToDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedDocumentDto downloadDocument(Long id) {
        CalibrationDocument document = calibrationDocumentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration document not found with id: " + id, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = document.getEquipment() != null && document.getEquipment().getLaboratory() != null
                ? document.getEquipment().getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to download calibration document in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        Path filePath = this.storageLocation.resolve(document.getFilePath()).normalize();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("Document file not found on storage server", HttpStatus.NOT_FOUND);
            }

            // Derive safe download filename
            String downloadFilename = document.getDocumentName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String extension = document.getFilePath().substring(document.getFilePath().lastIndexOf("."));
            if (!downloadFilename.toLowerCase().endsWith(extension.toLowerCase())) {
                downloadFilename += extension;
            }

            long contentLength = Files.size(filePath);
            return new DownloadedDocumentDto(resource, downloadFilename, document.getFileType(), contentLength);
        } catch (IOException e) {
            throw new ApiException("Error reading document file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalibrationDocumentResponseDto> getDocumentsByEquipment(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ApiException("Equipment not found with id: " + equipmentId, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = equipment.getLaboratory() != null ? equipment.getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access documents in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        return calibrationDocumentRepository.findByEquipmentIdOrderByUploadedAtDesc(equipmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalibrationDocumentResponseDto> getDocumentsByCalibrationRecord(Long calibrationRecordId) {
        CalibrationRecord record = calibrationRecordRepository.findById(calibrationRecordId)
                .orElseThrow(() -> new ApiException("Calibration record not found with id: " + calibrationRecordId, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = record.getEquipment() != null && record.getEquipment().getLaboratory() != null
                ? record.getEquipment().getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to access documents in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        return calibrationDocumentRepository.findByCalibrationRecordIdOrderByUploadedAtDesc(calibrationRecordId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalibrationDocumentResponseDto> getAllDocumentsWithFilters(Long equipmentId,
                                                                         Long calibrationRecordId,
                                                                         String certificateNumber,
                                                                         LocalDate issueStartDate,
                                                                         LocalDate issueEndDate,
                                                                         LocalDate validStartDate,
                                                                         LocalDate validEndDate) {
        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        if (equipmentId != null) {
            Equipment eq = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new ApiException("Equipment not found with id: " + equipmentId, HttpStatus.NOT_FOUND));
            Long eqLabId = eq.getLaboratory() != null ? eq.getLaboratory().getId() : null;
            if (eqLabId != null && !scope.isPermitted(eqLabId)) {
                throw new ApiException("Access denied: You do not have permission to access documents in laboratory ID: " + eqLabId, HttpStatus.FORBIDDEN);
            }
        }
        if (scope.isEmptyScope()) {
            return Collections.emptyList();
        }

        String certNum = StringUtils.hasText(certificateNumber) ? certificateNumber.trim() : null;
        List<CalibrationDocument> docs = calibrationDocumentRepository.findWithFilters(
                equipmentId, calibrationRecordId, certNum,
                issueStartDate, issueEndDate, validStartDate, validEndDate
        );

        if (scope.laboratoryIds() != null) {
            docs = docs.stream()
                    .filter(d -> d.getEquipment() != null && d.getEquipment().getLaboratory() != null && scope.isPermitted(d.getEquipment().getLaboratory().getId()))
                    .collect(Collectors.toList());
        }

        return docs.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        CalibrationDocument document = calibrationDocumentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Calibration document not found with id: " + id, HttpStatus.NOT_FOUND));

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope();
        Long labId = document.getEquipment() != null && document.getEquipment().getLaboratory() != null
                ? document.getEquipment().getLaboratory().getId() : null;
        if (labId != null && !scope.isPermitted(labId)) {
            throw new ApiException("Access denied: You do not have permission to delete document in laboratory ID: " + labId, HttpStatus.FORBIDDEN);
        }

        // Audit compliance: Documents linked to calibration records must NOT be deleted
        if (document.getCalibrationRecord() != null) {
            throw new ApiException(
                    "Cannot delete document associated with a calibration record as it is preserved for audit compliance",
                    HttpStatus.BAD_REQUEST);
        }

        // Delete metadata from DB
        calibrationDocumentRepository.delete(document);

        auditService.logEvent(
                AuditAction.CALIBRATION_DOCUMENT_DELETED,
                AuditEntityType.CALIBRATION_DOCUMENT,
                document.getId(),
                "Calibration document '" + document.getDocumentName() + "' (ID: " + document.getId() + ") was deleted.",
                Map.of(
                        "documentId", document.getId(),
                        "documentName", document.getDocumentName(),
                        "equipmentId", document.getEquipment().getId(),
                        "certificateNumber", document.getCertificateNumber() != null ? document.getCertificateNumber() : ""
                ),
                null
        );

        // Delete physical file
        Path filePath = this.storageLocation.resolve(document.getFilePath()).normalize();
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File is required", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new ApiException(
                    "File size exceeds maximum allowed limit of " + (maxFileSizeBytes / (1024 * 1024)) + "MB",
                    HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new ApiException("File name must not be empty", HttpStatus.BAD_REQUEST);
        }

        // Path traversal check on original filename
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new ApiException("Invalid file name containing path traversal characters", HttpStatus.BAD_REQUEST);
        }

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new ApiException("File must have a valid extension", HttpStatus.BAD_REQUEST);
        }

        String extension = originalFilename.substring(lastDotIndex).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(
                    "Unsupported file extension '" + extension + "'. Allowed extensions: PDF, PNG, JPEG/JPG",
                    HttpStatus.BAD_REQUEST);
        }

        String declaredContentType = file.getContentType();
        if (StringUtils.hasText(declaredContentType)) {
            String normalizedMime = declaredContentType.toLowerCase().trim();
            if (!ALLOWED_MIME_TYPES.contains(normalizedMime) && !normalizedMime.equals("image/pjpeg")) {
                throw new ApiException(
                        "Unsupported MIME type '" + declaredContentType + "'. Allowed MIME types: application/pdf, image/png, image/jpeg",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Validate Magic Bytes (File Signatures)
        validateFileSignature(file, extension);

        return extension;
    }

    private void validateFileSignature(MultipartFile file, String extension) {
        byte[] header = new byte[8];
        int bytesRead;
        try (InputStream is = file.getInputStream()) {
            bytesRead = is.read(header);
        } catch (IOException e) {
            throw new ApiException("Unable to read file content for signature verification", HttpStatus.BAD_REQUEST);
        }

        if (bytesRead < 3) {
            throw new ApiException("File is too small to be a valid document or image", HttpStatus.BAD_REQUEST);
        }

        switch (extension) {
            case ".pdf" -> {
                if (bytesRead < PDF_MAGIC.length) {
                    throw new ApiException("Invalid file content: file does not match PDF format signature", HttpStatus.BAD_REQUEST);
                }
                for (int i = 0; i < PDF_MAGIC.length; i++) {
                    if (header[i] != PDF_MAGIC[i]) {
                        throw new ApiException("Invalid file content: file does not match PDF format signature", HttpStatus.BAD_REQUEST);
                    }
                }
            }
            case ".png" -> {
                if (bytesRead < PNG_MAGIC.length) {
                    throw new ApiException("Invalid file content: file does not match PNG format signature", HttpStatus.BAD_REQUEST);
                }
                for (int i = 0; i < PNG_MAGIC.length; i++) {
                    if (header[i] != PNG_MAGIC[i]) {
                        throw new ApiException("Invalid file content: file does not match PNG format signature", HttpStatus.BAD_REQUEST);
                    }
                }
            }
            case ".jpg", ".jpeg" -> {
                if (bytesRead < JPEG_MAGIC.length) {
                    throw new ApiException("Invalid file content: file does not match JPEG format signature", HttpStatus.BAD_REQUEST);
                }
                for (int i = 0; i < JPEG_MAGIC.length; i++) {
                    if (header[i] != JPEG_MAGIC[i]) {
                        throw new ApiException("Invalid file content: file does not match JPEG format signature", HttpStatus.BAD_REQUEST);
                    }
                }
            }
            default -> throw new ApiException("Unsupported file format", HttpStatus.BAD_REQUEST);
        }
    }

    private String resolveMimeType(String extension, String fallbackContentType) {
        return switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> StringUtils.hasText(fallbackContentType) ? fallbackContentType : "application/octet-stream";
        };
    }

    private CalibrationDocumentResponseDto mapToDto(CalibrationDocument document) {
        String validityStatus;
        if (document.getValidUntil() == null) {
            validityStatus = "NO_EXPIRY";
        } else if (document.getValidUntil().isBefore(LocalDate.now())) {
            validityStatus = "EXPIRED";
        } else {
            validityStatus = "VALID";
        }

        Long recordId = document.getCalibrationRecord() != null ? document.getCalibrationRecord().getId() : null;
        LocalDate calibrationDate = document.getCalibrationRecord() != null ? document.getCalibrationRecord().getCalibrationDate() : null;

        return new CalibrationDocumentResponseDto(
                document.getId(),
                document.getEquipment().getId(),
                document.getEquipment().getName(),
                document.getEquipment().getAssetTag(),
                document.getEquipment().getLaboratory().getId(),
                document.getEquipment().getLaboratory().getName(),
                recordId,
                calibrationDate,
                document.getDocumentName(),
                document.getCertificateNumber(),
                document.getFileType(),
                document.getFileSizeBytes(),
                document.getIssueDate(),
                document.getValidUntil(),
                validityStatus,
                document.getUploadedAt(),
                document.getCreatedAt()
        );
    }

    private static final Map<String, String> DOCUMENT_SORT_FIELDS = Map.of(
            "documentName", "documentName",
            "certificateNumber", "certificateNumber",
            "issueDate", "issueDate",
            "validUntil", "validUntil",
            "uploadedAt", "uploadedAt",
            "fileSize", "fileSizeBytes"
    );

    @Override
    @Transactional(readOnly = true)
    public com.calibratesys.dto.PageResponseDto<CalibrationDocumentResponseDto> searchDocuments(
            Long equipmentId,
            Long laboratoryId,
            Long calibrationRecordId,
            String certificateNumber,
            String documentName,
            String fileType,
            String validity,
            LocalDate issueStartDate,
            LocalDate issueEndDate,
            LocalDate validStartDate,
            LocalDate validEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        if (issueStartDate != null && issueEndDate != null && issueStartDate.isAfter(issueEndDate)) {
            throw new ApiException("Issue start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }
        if (validStartDate != null && validEndDate != null && validStartDate.isAfter(validEndDate)) {
            throw new ApiException("Valid start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate effValidStart = validStartDate;
        LocalDate effValidEnd = validEndDate;

        if (org.springframework.util.StringUtils.hasText(validity)) {
            String v = validity.trim().toUpperCase();
            switch (v) {
                case "VALID" -> {
                    effValidStart = (effValidStart != null && effValidStart.isAfter(today)) ? effValidStart : today;
                }
                case "EXPIRED" -> {
                    LocalDate yesterday = today.minusDays(1);
                    effValidEnd = (effValidEnd != null && effValidEnd.isBefore(yesterday)) ? effValidEnd : yesterday;
                }
                case "EXPIRING_SOON" -> {
                    LocalDate in30Days = today.plusDays(30);
                    effValidStart = (effValidStart != null && effValidStart.isAfter(today)) ? effValidStart : today;
                    effValidEnd = (effValidEnd != null && effValidEnd.isBefore(in30Days)) ? effValidEnd : in30Days;
                }
                default -> throw new ApiException("Invalid validity filter: '" + validity + "'. Allowed values are: VALID, EXPIRED, EXPIRING_SOON", HttpStatus.BAD_REQUEST);
            }
        }

        String certNum = org.springframework.util.StringUtils.hasText(certificateNumber) ? certificateNumber.trim() : null;
        String docName = org.springframework.util.StringUtils.hasText(documentName) ? documentName.trim() : null;
        String fType = org.springframework.util.StringUtils.hasText(fileType) ? fileType.trim() : null;
        String q = org.springframework.util.StringUtils.hasText(search) ? search.trim() : null;

        org.springframework.data.domain.Sort sort = com.calibratesys.util.SortValidator.validateAndBuildSort(
                sortBy, sortDirection, "uploadedAt", org.springframework.data.domain.Sort.Direction.DESC, DOCUMENT_SORT_FIELDS
        );

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(size, 100));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(validatedPage, validatedSize, sort);

        com.calibratesys.security.ScopedContext scope = dataScopeService.resolveScope(laboratoryId);
        if (scope.isEmptyScope()) {
            return com.calibratesys.dto.PageResponseDto.from(new org.springframework.data.domain.PageImpl<>(java.util.Collections.emptyList(), pageable, 0));
        }

        org.springframework.data.domain.Page<CalibrationDocument> pageResult;
        if (scope.laboratoryIds() != null) {
            pageResult = calibrationDocumentRepository.findWithAdvancedFiltersAndLaboratoryIds(
                    scope.laboratoryIds(), equipmentId, laboratoryId, calibrationRecordId,
                    certNum, docName, fType, issueStartDate, issueEndDate,
                    effValidStart, effValidEnd, q, pageable
            );
        } else {
            pageResult = calibrationDocumentRepository.findWithAdvancedFilters(
                    equipmentId, laboratoryId, calibrationRecordId,
                    certNum, docName, fType, issueStartDate, issueEndDate,
                    effValidStart, effValidEnd, q, pageable
            );
        }

        return com.calibratesys.dto.PageResponseDto.from(pageResult.map(this::mapToDto));
    }
}
