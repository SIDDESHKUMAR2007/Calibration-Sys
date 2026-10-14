package com.calibratesys.dto;

import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;

import java.time.LocalDate;

public class DashboardCalibrationItemDto {

    private Long scheduleId;
    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private Long laboratoryId;
    private String laboratoryName;
    private String laboratoryCode;
    private LocalDate scheduledDate;
    private LocalDate dueDate;
    private ScheduleStatus status;
    private SchedulePriority priority;
    private Long daysOverdue;
    private Long daysUntilDue;
    private String responsibleStaffName;
    private String responsibleStaffRole;

    public DashboardCalibrationItemDto() {
    }

    public DashboardCalibrationItemDto(Long scheduleId, Long equipmentId, String equipmentName, String assetTag,
                                       Long laboratoryId, String laboratoryName, String laboratoryCode,
                                       LocalDate scheduledDate, LocalDate dueDate, ScheduleStatus status,
                                       SchedulePriority priority, Long daysOverdue, Long daysUntilDue,
                                       String responsibleStaffName, String responsibleStaffRole) {
        this.scheduleId = scheduleId;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.assetTag = assetTag;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.laboratoryCode = laboratoryCode;
        this.scheduledDate = scheduledDate;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.daysOverdue = daysOverdue;
        this.daysUntilDue = daysUntilDue;
        this.responsibleStaffName = responsibleStaffName;
        this.responsibleStaffRole = responsibleStaffRole;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
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

    public String getLaboratoryCode() {
        return laboratoryCode;
    }

    public void setLaboratoryCode(String laboratoryCode) {
        this.laboratoryCode = laboratoryCode;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public SchedulePriority getPriority() {
        return priority;
    }

    public void setPriority(SchedulePriority priority) {
        this.priority = priority;
    }

    public Long getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(Long daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public Long getDaysUntilDue() {
        return daysUntilDue;
    }

    public void setDaysUntilDue(Long daysUntilDue) {
        this.daysUntilDue = daysUntilDue;
    }

    public String getResponsibleStaffName() {
        return responsibleStaffName;
    }

    public void setResponsibleStaffName(String responsibleStaffName) {
        this.responsibleStaffName = responsibleStaffName;
    }

    public String getResponsibleStaffRole() {
        return responsibleStaffRole;
    }

    public void setResponsibleStaffRole(String responsibleStaffRole) {
        this.responsibleStaffRole = responsibleStaffRole;
    }
}
