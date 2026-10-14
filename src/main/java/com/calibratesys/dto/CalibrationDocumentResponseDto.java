package com.calibratesys.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CalibrationDocumentResponseDto {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCode;
    private Long laboratoryId;
    private String laboratoryName;
    private Long calibrationRecordId;
    private LocalDate calibrationDate;
    private String documentName;
    private String certificateNumber;
    private String fileType;
    private Long fileSizeBytes;
    private LocalDate issueDate;
    private LocalDate validUntil;
    private String validityStatus;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;

    public CalibrationDocumentResponseDto() {
    }

    public CalibrationDocumentResponseDto(Long id, Long equipmentId, String equipmentName, String equipmentCode,
                                          Long laboratoryId, String laboratoryName, Long calibrationRecordId,
                                          LocalDate calibrationDate, String documentName, String certificateNumber,
                                          String fileType, Long fileSizeBytes, LocalDate issueDate,
                                          LocalDate validUntil, String validityStatus, LocalDateTime uploadedAt,
                                          LocalDateTime createdAt) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.equipmentCode = equipmentCode;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.calibrationRecordId = calibrationRecordId;
        this.calibrationDate = calibrationDate;
        this.documentName = documentName;
        this.certificateNumber = certificateNumber;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.issueDate = issueDate;
        this.validUntil = validUntil;
        this.validityStatus = validityStatus;
        this.uploadedAt = uploadedAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }

    public Long getLaboratoryId() {
        return laboratoryId;
    }

    public void setLaboratoryId(Long laboratoryId) {
        this.laboratoryId = laboratoryId;
    }

    public String getLaboratoryName() {
        return laboratoryName;
    }

    public void setLaboratoryName(String laboratoryName) {
        this.laboratoryName = laboratoryName;
    }

    public Long getCalibrationRecordId() {
        return calibrationRecordId;
    }

    public void setCalibrationRecordId(Long calibrationRecordId) {
        this.calibrationRecordId = calibrationRecordId;
    }

    public LocalDate getCalibrationDate() {
        return calibrationDate;
    }

    public void setCalibrationDate(LocalDate calibrationDate) {
        this.calibrationDate = calibrationDate;
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

    public String getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(String validityStatus) {
        this.validityStatus = validityStatus;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
