package com.calibratesys.dto;

import java.time.LocalDate;

public class ComplianceAttentionReportDto {

    private String category;
    private String severity;
    private Long equipmentId;
    private String assetTag;
    private String equipmentName;
    private Long laboratoryId;
    private String laboratoryName;
    private String details;
    private String actionRequired;
    private LocalDate relevantDate;

    public ComplianceAttentionReportDto() {
    }

    public ComplianceAttentionReportDto(String category, String severity, Long equipmentId,
                                        String assetTag, String equipmentName, Long laboratoryId,
                                        String laboratoryName, String details, String actionRequired,
                                        LocalDate relevantDate) {
        this.category = category;
        this.severity = severity;
        this.equipmentId = equipmentId;
        this.assetTag = assetTag;
        this.equipmentName = equipmentName;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.details = details;
        this.actionRequired = actionRequired;
        this.relevantDate = relevantDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(String actionRequired) {
        this.actionRequired = actionRequired;
    }

    public LocalDate getRelevantDate() {
        return relevantDate;
    }

    public void setRelevantDate(LocalDate relevantDate) {
        this.relevantDate = relevantDate;
    }
}
