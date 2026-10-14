package com.calibratesys.dto;

import com.calibratesys.entity.enums.LabStatus;

public class LaboratoryDashboardSummaryDto {

    private Long laboratoryId;
    private String laboratoryName;
    private String laboratoryCode;
    private String collegeName;
    private LabStatus status;
    private Long totalEquipment;
    private Long operationalEquipment;
    private Long upcomingCalibrationCount;
    private Long dueCount;
    private Long overdueCount;
    private boolean hasOverdue;

    public LaboratoryDashboardSummaryDto() {
    }

    public LaboratoryDashboardSummaryDto(Long laboratoryId, String laboratoryName, String laboratoryCode,
                                         String collegeName, LabStatus status, Long totalEquipment,
                                         Long operationalEquipment, Long upcomingCalibrationCount,
                                         Long dueCount, Long overdueCount, boolean hasOverdue) {
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.laboratoryCode = laboratoryCode;
        this.collegeName = collegeName;
        this.status = status;
        this.totalEquipment = totalEquipment;
        this.operationalEquipment = operationalEquipment;
        this.upcomingCalibrationCount = upcomingCalibrationCount;
        this.dueCount = dueCount;
        this.overdueCount = overdueCount;
        this.hasOverdue = hasOverdue;
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

    public String getLaboratoryCode() {
        return laboratoryCode;
    }

    public void setLaboratoryCode(String laboratoryCode) {
        this.laboratoryCode = laboratoryCode;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public LabStatus getStatus() {
        return status;
    }

    public void setStatus(LabStatus status) {
        this.status = status;
    }

    public Long getTotalEquipment() {
        return totalEquipment;
    }

    public void setTotalEquipment(Long totalEquipment) {
        this.totalEquipment = totalEquipment;
    }

    public Long getOperationalEquipment() {
        return operationalEquipment;
    }

    public void setOperationalEquipment(Long operationalEquipment) {
        this.operationalEquipment = operationalEquipment;
    }

    public Long getUpcomingCalibrationCount() {
        return upcomingCalibrationCount;
    }

    public void setUpcomingCalibrationCount(Long upcomingCalibrationCount) {
        this.upcomingCalibrationCount = upcomingCalibrationCount;
    }

    public Long getDueCount() {
        return dueCount;
    }

    public void setDueCount(Long dueCount) {
        this.dueCount = dueCount;
    }

    public Long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Long overdueCount) {
        this.overdueCount = overdueCount;
    }

    public boolean isHasOverdue() {
        return hasOverdue;
    }

    public void setHasOverdue(boolean hasOverdue) {
        this.hasOverdue = hasOverdue;
    }
}
