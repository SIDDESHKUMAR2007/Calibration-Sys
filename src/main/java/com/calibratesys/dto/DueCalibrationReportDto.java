package com.calibratesys.dto;

import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;

import java.time.LocalDate;

public class DueCalibrationReportDto {

    private Long scheduleId;
    private Long equipmentId;
    private String assetTag;
    private String equipmentName;
    private Long laboratoryId;
    private String laboratoryName;
    private LocalDate scheduledDate;
    private LocalDate dueDate;
    private SchedulePriority priority;
    private ScheduleStatus scheduleStatus;
    private String responsibleStaff;

    public DueCalibrationReportDto() {
    }

    public DueCalibrationReportDto(Long scheduleId, Long equipmentId, String assetTag,
                                   String equipmentName, Long laboratoryId, String laboratoryName,
                                   LocalDate scheduledDate, LocalDate dueDate,
                                   SchedulePriority priority, ScheduleStatus scheduleStatus,
                                   String responsibleStaff) {
        this.scheduleId = scheduleId;
        this.equipmentId = equipmentId;
        this.assetTag = assetTag;
        this.equipmentName = equipmentName;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.scheduledDate = scheduledDate;
        this.dueDate = dueDate;
        this.priority = priority;
        this.scheduleStatus = scheduleStatus;
        this.responsibleStaff = responsibleStaff;
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

    public SchedulePriority getPriority() {
        return priority;
    }

    public void setPriority(SchedulePriority priority) {
        this.priority = priority;
    }

    public ScheduleStatus getScheduleStatus() {
        return scheduleStatus;
    }

    public void setScheduleStatus(ScheduleStatus scheduleStatus) {
        this.scheduleStatus = scheduleStatus;
    }

    public String getResponsibleStaff() {
        return responsibleStaff;
    }

    public void setResponsibleStaff(String responsibleStaff) {
        this.responsibleStaff = responsibleStaff;
    }
}
