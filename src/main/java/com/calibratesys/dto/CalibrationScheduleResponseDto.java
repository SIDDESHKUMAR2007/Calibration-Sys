package com.calibratesys.dto;

import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CalibrationScheduleResponseDto {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String assetTag;
    private String serialNumber;
    private Long laboratoryId;
    private String laboratoryCode;
    private String laboratoryName;
    private Long collegeId;
    private String collegeCode;
    private String collegeName;
    private LocalDate scheduledDate;
    private LocalDate dueDate;
    private ScheduleStatus status;
    private SchedulePriority priority;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CalibrationScheduleResponseDto() {
    }

    public CalibrationScheduleResponseDto(Long id, Long equipmentId, String equipmentName, String assetTag,
                                         String serialNumber, Long laboratoryId, String laboratoryCode,
                                         String laboratoryName, Long collegeId, String collegeCode,
                                         String collegeName, LocalDate scheduledDate, LocalDate dueDate,
                                         ScheduleStatus status, SchedulePriority priority, String remarks,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.assetTag = assetTag;
        this.serialNumber = serialNumber;
        this.laboratoryId = laboratoryId;
        this.laboratoryCode = laboratoryCode;
        this.laboratoryName = laboratoryName;
        this.collegeId = collegeId;
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
        this.scheduledDate = scheduledDate;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.remarks = remarks;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
