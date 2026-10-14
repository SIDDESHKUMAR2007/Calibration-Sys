package com.calibratesys.dto;

import com.calibratesys.entity.enums.EquipmentStatus;

import java.time.LocalDate;

public class EquipmentReportDto {

    private Long equipmentId;
    private String assetTag;
    private String serialNumber;
    private String equipmentName;
    private String modelNumber;
    private String manufacturer;
    private Long laboratoryId;
    private String laboratoryName;
    private LocalDate installationDate;
    private Integer calibrationFrequencyDays;
    private EquipmentStatus currentStatus;
    private LocalDate latestCalibrationDate;
    private LocalDate nextDueDate;

    public EquipmentReportDto() {
    }

    public EquipmentReportDto(Long equipmentId, String assetTag, String serialNumber,
                              String equipmentName, String modelNumber, String manufacturer,
                              Long laboratoryId, String laboratoryName, LocalDate installationDate,
                              Integer calibrationFrequencyDays, EquipmentStatus currentStatus,
                              LocalDate latestCalibrationDate, LocalDate nextDueDate) {
        this.equipmentId = equipmentId;
        this.assetTag = assetTag;
        this.serialNumber = serialNumber;
        this.equipmentName = equipmentName;
        this.modelNumber = modelNumber;
        this.manufacturer = manufacturer;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.installationDate = installationDate;
        this.calibrationFrequencyDays = calibrationFrequencyDays;
        this.currentStatus = currentStatus;
        this.latestCalibrationDate = latestCalibrationDate;
        this.nextDueDate = nextDueDate;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
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

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
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

    public EquipmentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(EquipmentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public LocalDate getLatestCalibrationDate() {
        return latestCalibrationDate;
    }

    public void setLatestCalibrationDate(LocalDate latestCalibrationDate) {
        this.latestCalibrationDate = latestCalibrationDate;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }
}
