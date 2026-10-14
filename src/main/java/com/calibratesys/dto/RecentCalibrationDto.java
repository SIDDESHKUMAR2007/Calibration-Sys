package com.calibratesys.dto;

import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;

import java.time.LocalDate;

public class RecentCalibrationDto {

    private Long recordId;
    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private Long laboratoryId;
    private String laboratoryName;
    private LocalDate calibrationDate;
    private LocalDate nextDueDate;
    private CalibrationResult result;
    private CalibratedByType calibratedByType;
    private String calibratorName;
    private boolean hasCertificate;
    private int documentCount;

    public RecentCalibrationDto() {
    }

    public RecentCalibrationDto(Long recordId, Long equipmentId, String equipmentName, String assetTag,
                                Long laboratoryId, String laboratoryName, LocalDate calibrationDate,
                                LocalDate nextDueDate, CalibrationResult result, CalibratedByType calibratedByType,
                                String calibratorName, boolean hasCertificate, int documentCount) {
        this.recordId = recordId;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.assetTag = assetTag;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.calibrationDate = calibrationDate;
        this.nextDueDate = nextDueDate;
        this.result = result;
        this.calibratedByType = calibratedByType;
        this.calibratorName = calibratorName;
        this.hasCertificate = hasCertificate;
        this.documentCount = documentCount;
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

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
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

    public CalibrationResult getResult() {
        return result;
    }

    public void setResult(CalibrationResult result) {
        this.result = result;
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

    public boolean isHasCertificate() {
        return hasCertificate;
    }

    public void setHasCertificate(boolean hasCertificate) {
        this.hasCertificate = hasCertificate;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }
}
