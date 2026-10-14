package com.calibratesys.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "calibration_documents", indexes = {
    @Index(name = "idx_calib_doc_equip", columnList = "equipment_id"),
    @Index(name = "idx_calib_doc_record", columnList = "calibration_record_id"),
    @Index(name = "idx_calib_doc_cert_no", columnList = "certificate_number")
})
public class CalibrationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calibration_record_id")
    private CalibrationRecord calibrationRecord;

    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    @Column(name = "certificate_number", length = 100)
    private String certificateNumber;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CalibrationDocument() {
    }

    public CalibrationDocument(Equipment equipment, CalibrationRecord calibrationRecord,
                               String documentName, String certificateNumber, String filePath,
                               String fileType, Long fileSizeBytes, LocalDate issueDate, LocalDate validUntil) {
        this.equipment = equipment;
        this.calibrationRecord = calibrationRecord;
        this.documentName = documentName;
        this.certificateNumber = certificateNumber;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.issueDate = issueDate;
        this.validUntil = validUntil;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public CalibrationRecord getCalibrationRecord() {
        return calibrationRecord;
    }

    public void setCalibrationRecord(CalibrationRecord calibrationRecord) {
        this.calibrationRecord = calibrationRecord;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
