package com.calibratesys.dto;

import com.calibratesys.entity.enums.CalibrationResult;
import com.calibratesys.entity.enums.EquipmentStatus;
import com.calibratesys.entity.enums.SchedulePriority;

import java.time.LocalDate;

public class CalibrationStatusReportDto {

    private Long equipmentId;
    private String assetTag;
    private String equipmentName;
    private Long laboratoryId;
    private String laboratoryName;
    private EquipmentStatus equipmentStatus;
    private LocalDate latestCalibrationDate;
    private LocalDate nextDueDate;
    private CalibrationResult latestCalibrationResult;
    private SchedulePriority calibrationPriority;

    public CalibrationStatusReportDto() {
    }

    public CalibrationStatusReportDto(Long equipmentId, String assetTag, String equipmentName,
                                      Long laboratoryId, String laboratoryName,
                                      EquipmentStatus equipmentStatus, LocalDate latestCalibrationDate,
                                      LocalDate nextDueDate, CalibrationResult latestCalibrationResult,
                                      SchedulePriority calibrationPriority) {
        this.equipmentId = equipmentId;
        this.assetTag = assetTag;
        this.equipmentName = equipmentName;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.equipmentStatus = equipmentStatus;
        this.latestCalibrationDate = latestCalibrationDate;
        this.nextDueDate = nextDueDate;
        this.latestCalibrationResult = latestCalibrationResult;
        this.calibrationPriority = calibrationPriority;
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

    public EquipmentStatus getEquipmentStatus() {
        return equipmentStatus;
    }

    public void setEquipmentStatus(EquipmentStatus equipmentStatus) {
        this.equipmentStatus = equipmentStatus;
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

    public CalibrationResult getLatestCalibrationResult() {
        return latestCalibrationResult;
    }

    public void setLatestCalibrationResult(CalibrationResult latestCalibrationResult) {
        this.latestCalibrationResult = latestCalibrationResult;
    }

    public SchedulePriority getCalibrationPriority() {
        return calibrationPriority;
    }

    public void setCalibrationPriority(SchedulePriority calibrationPriority) {
        this.calibrationPriority = calibrationPriority;
    }
}
