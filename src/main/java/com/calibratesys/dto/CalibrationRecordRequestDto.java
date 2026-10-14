package com.calibratesys.dto;

import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CalibrationRecordRequestDto {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    private Long scheduleId;

    @NotNull(message = "Calibration date is required")
    private LocalDate calibrationDate;

    @NotNull(message = "Next due date is required")
    private LocalDate nextDueDate;

    @NotNull(message = "Calibrated by type is required")
    private CalibratedByType calibratedByType;

    @NotBlank(message = "Calibrator name is required")
    @Size(max = 150, message = "Calibrator name cannot exceed 150 characters")
    private String calibratorName;

    @NotNull(message = "Calibration result is required")
    private CalibrationResult result;

    @Size(max = 100, message = "Uncertainty measurement cannot exceed 100 characters")
    private String uncertaintyMeasurement;

    @Size(max = 255, message = "Standards used cannot exceed 255 characters")
    private String standardsUsed;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    public CalibrationRecordRequestDto() {
    }

    public CalibrationRecordRequestDto(Long equipmentId, Long scheduleId, LocalDate calibrationDate,
                                       LocalDate nextDueDate, CalibratedByType calibratedByType,
                                       String calibratorName, CalibrationResult result,
                                       String uncertaintyMeasurement, String standardsUsed, String notes) {
        this.equipmentId = equipmentId;
        this.scheduleId = scheduleId;
        this.calibrationDate = calibrationDate;
        this.nextDueDate = nextDueDate;
        this.calibratedByType = calibratedByType;
        this.calibratorName = calibratorName;
        this.result = result;
        this.uncertaintyMeasurement = uncertaintyMeasurement;
        this.standardsUsed = standardsUsed;
        this.notes = notes;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
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
}
