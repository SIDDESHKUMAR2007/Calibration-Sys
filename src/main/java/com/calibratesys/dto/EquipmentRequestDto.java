package com.calibratesys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for creating or updating Laboratory Equipment.
 */
public class EquipmentRequestDto {

    @NotNull(message = "Laboratory ID is required")
    private Long laboratoryId;

    @NotBlank(message = "Asset tag is required")
    @Size(min = 1, max = 100, message = "Asset tag must not exceed 100 characters")
    private String assetTag;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serialNumber;

    @NotBlank(message = "Equipment name is required")
    @Size(min = 1, max = 150, message = "Equipment name must not exceed 150 characters")
    private String name;

    @Size(max = 100, message = "Model number must not exceed 100 characters")
    private String modelNumber;

    @Size(max = 100, message = "Manufacturer must not exceed 100 characters")
    private String manufacturer;

    private LocalDate purchaseDate;

    private LocalDate installationDate;

    @NotNull(message = "Calibration frequency is required")
    @Positive(message = "Calibration frequency must be a positive number of days")
    private Integer calibrationFrequencyDays;

    private String status;

    public EquipmentRequestDto() {
    }

    public EquipmentRequestDto(Long laboratoryId, String assetTag, String serialNumber,
                               String name, String modelNumber, String manufacturer,
                               LocalDate purchaseDate, LocalDate installationDate,
                               Integer calibrationFrequencyDays, String status) {
        this.laboratoryId = laboratoryId;
        this.assetTag = assetTag;
        this.serialNumber = serialNumber;
        this.name = name;
        this.modelNumber = modelNumber;
        this.manufacturer = manufacturer;
        this.purchaseDate = purchaseDate;
        this.installationDate = installationDate;
        this.calibrationFrequencyDays = calibrationFrequencyDays;
        this.status = status;
    }

    public Long getLaboratoryId() {
        return laboratoryId;
    }

    public void setLaboratoryId(Long laboratoryId) {
        this.laboratoryId = laboratoryId;
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
}
