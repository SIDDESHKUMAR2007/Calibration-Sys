package com.calibratesys.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Public response projection for Laboratory Equipment details.
 */
public class EquipmentResponseDto {

    private Long id;
    private Long laboratoryId;
    private String laboratoryCode;
    private String laboratoryName;
    private Long collegeId;
    private String collegeCode;
    private String collegeName;
    private String assetTag;
    private String serialNumber;
    private String name;
    private String modelNumber;
    private String manufacturer;
    private LocalDate purchaseDate;
    private LocalDate installationDate;
    private Integer calibrationFrequencyDays;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EquipmentResponseDto() {
    }

    public EquipmentResponseDto(Long id, Long laboratoryId, String laboratoryCode, String laboratoryName,
                                Long collegeId, String collegeCode, String collegeName,
                                String assetTag, String serialNumber, String name,
                                String modelNumber, String manufacturer, LocalDate purchaseDate,
                                LocalDate installationDate, Integer calibrationFrequencyDays,
                                String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.laboratoryCode = laboratoryCode;
        this.laboratoryName = laboratoryName;
        this.collegeId = collegeId;
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
        this.assetTag = assetTag;
        this.serialNumber = serialNumber;
        this.name = name;
        this.modelNumber = modelNumber;
        this.manufacturer = manufacturer;
        this.purchaseDate = purchaseDate;
        this.installationDate = installationDate;
        this.calibrationFrequencyDays = calibrationFrequencyDays;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLaboratoryId() {
        return laboratoryId;
    }

    public void setLaboratoryId(Long laboratoryId) {
        this.laboratoryId = laboratoryId;
    }

    public String getLaboratoryCode() {
        return laboratoryCode;
    }

    public void setLaboratoryCode(String laboratoryCode) {
        this.laboratoryCode = laboratoryCode;
    }

    public String getLaboratoryName() {
        return laboratoryName;
    }

    public void setLaboratoryName(String laboratoryName) {
        this.laboratoryName = laboratoryName;
    }

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(LocalDate installationDate) {
        this.installationDate = installationDate;
    }

    public Integer getCalibrationFrequencyDays() {
        return calibrationFrequencyDays;
    }

    public void setCalibrationFrequencyDays(Integer calibrationFrequencyDays) {
        this.calibrationFrequencyDays = calibrationFrequencyDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
