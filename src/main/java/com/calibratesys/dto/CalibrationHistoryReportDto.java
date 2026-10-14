package com.calibratesys.dto;

import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;

import java.time.LocalDate;

public class CalibrationHistoryReportDto {

    private Long recordId;
    private Long equipmentId;
    private String assetTag;
    private String equipmentName;
    private Long laboratoryId;
    private String laboratoryName;
    private LocalDate calibrationDate;
    private LocalDate nextDueDate;
    private CalibrationResult calibrationResult;
    private CalibratedByType calibratorType;
    private String calibratorName;
    private String standardsUsed;
    private String uncertaintyMeasurement;
    private String notes;

    public CalibrationHistoryReportDto() {
    }

    public CalibrationHistoryReportDto(Long recordId, Long equipmentId, String assetTag,
                                       String equipmentName, Long laboratoryId, String laboratoryName,
                                       LocalDate calibrationDate, LocalDate nextDueDate,
                                       CalibrationResult calibrationResult, CalibratedByType calibratorType,
                                       String calibratorName, String standardsUsed,
                                       String uncertaintyMeasurement, String notes) {
        this.recordId = recordId;
        this.equipmentId = equipmentId;
        this.assetTag = assetTag;
        this.equipmentName = equipmentName;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.calibrationDate = calibrationDate;
        this.nextDueDate = nextDueDate;
        this.calibrationResult = calibrationResult;
        this.calibratorType = calibratorType;
        this.calibratorName = calibratorName;
        this.standardsUsed = standardsUsed;
        this.uncertaintyMeasurement = uncertaintyMeasurement;
        this.notes = notes;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
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

    public CalibrationResult getCalibrationResult() {
        return calibrationResult;
    }

    public void setCalibrationResult(CalibrationResult calibrationResult) {
        this.calibrationResult = calibrationResult;
    }

    public CalibratedByType getCalibratorType() {
        return calibratorType;
    }

    public void setCalibratorType(CalibratedByType calibratorType) {
        this.calibratorType = calibratorType;
    }

    public String getCalibratorName() {
        return calibratorName;
    }

    public void setCalibratorName(String calibratorName) {
        this.calibratorName = calibratorName;
    }

    public String getStandardsUsed() {
        return standardsUsed;
    }

    public void setStandardsUsed(String standardsUsed) {
        this.standardsUsed = standardsUsed;
    }

    public String getUncertaintyMeasurement() {
        return uncertaintyMeasurement;
    }

    public void setUncertaintyMeasurement(String uncertaintyMeasurement) {
        this.uncertaintyMeasurement = uncertaintyMeasurement;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
