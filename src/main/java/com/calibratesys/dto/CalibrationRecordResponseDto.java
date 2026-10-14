package com.calibratesys.dto;

import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CalibrationRecordResponseDto {

    private Long id;
    private Long equipmentId;
    private String assetTag;
    private String equipmentName;
    private String serialNumber;
    private Long laboratoryId;
    private String laboratoryCode;
    private String laboratoryName;
    private Long collegeId;
    private String collegeCode;
    private String collegeName;
    private Long scheduleId;
    private LocalDate calibrationDate;
    private LocalDate nextDueDate;
    private CalibratedByType calibratedByType;
    private String calibratorName;
    private CalibrationResult result;
    private String uncertaintyMeasurement;
    private String standardsUsed;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CalibrationRecordResponseDto() {
    }

    public CalibrationRecordResponseDto(Long id, Long equipmentId, String assetTag, String equipmentName,
                                        String serialNumber, Long laboratoryId, String laboratoryCode,
                                        String laboratoryName, Long collegeId, String collegeCode,
                                        String collegeName, Long scheduleId, LocalDate calibrationDate,
                                        LocalDate nextDueDate, CalibratedByType calibratedByType,
                                        String calibratorName, CalibrationResult result,
                                        String uncertaintyMeasurement, String standardsUsed, String notes,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.assetTag = assetTag;
        this.equipmentName = equipmentName;
        this.serialNumber = serialNumber;
        this.laboratoryId = laboratoryId;
        this.laboratoryCode = laboratoryCode;
        this.laboratoryName = laboratoryName;
        this.collegeId = collegeId;
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
        this.scheduleId = scheduleId;
        this.calibrationDate = calibrationDate;
        this.nextDueDate = nextDueDate;
        this.calibratedByType = calibratedByType;
        this.calibratorName = calibratorName;
        this.result = result;
        this.uncertaintyMeasurement = uncertaintyMeasurement;
        this.standardsUsed = standardsUsed;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public LocalDate getCalibrationDate() {
        return calibrationDate;
    }

    public void setCalibrationDate(LocalDate calibrationDate) {
        this.calibrationDate = calibrationDate;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public CalibratedByType getCalibratedByType() {
        return calibratedByType;
    }

    public void setCalibratedByType(CalibratedByType calibratedByType) {
        this.calibratedByType = calibratedByType;
    }

    public String getCalibratorName() {
        return calibratorName;
    }

    public void setCalibratorName(String calibratorName) {
        this.calibratorName = calibratorName;
    }

    public CalibrationResult getResult() {
        return result;
    }

    public void setResult(CalibrationResult result) {
        this.result = result;
    }

    public String getUncertaintyMeasurement() {
        return uncertaintyMeasurement;
    }

    public void setUncertaintyMeasurement(String uncertaintyMeasurement) {
        this.uncertaintyMeasurement = uncertaintyMeasurement;
    }

    public String getStandardsUsed() {
        return standardsUsed;
    }

    public void setStandardsUsed(String standardsUsed) {
        this.standardsUsed = standardsUsed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
