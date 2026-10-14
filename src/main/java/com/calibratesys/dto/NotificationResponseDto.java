package com.calibratesys.dto;

import com.calibratesys.entity.enums.NotificationPriority;
import com.calibratesys.entity.enums.NotificationType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotificationResponseDto {

    private Long id;
    private Long recipientStaffId;
    private String recipientStaffName;
    private String title;
    private String message;
    private NotificationType notificationType;
    private NotificationPriority priority;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private Long laboratoryId;
    private String laboratoryName;
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCode;
    private Long calibrationScheduleId;
    private LocalDate scheduledDueDate;

    public NotificationResponseDto() {
    }

    public NotificationResponseDto(Long id, Long recipientStaffId, String recipientStaffName,
                                   String title, String message, NotificationType notificationType,
                                   NotificationPriority priority, boolean isRead, LocalDateTime readAt,
                                   LocalDateTime createdAt, Long laboratoryId, String laboratoryName,
                                   Long equipmentId, String equipmentName, String equipmentCode,
                                   Long calibrationScheduleId, LocalDate scheduledDueDate) {
        this.id = id;
        this.recipientStaffId = recipientStaffId;
        this.recipientStaffName = recipientStaffName;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.priority = priority;
        this.isRead = isRead;
        this.readAt = readAt;
        this.createdAt = createdAt;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.equipmentCode = equipmentCode;
        this.calibrationScheduleId = calibrationScheduleId;
        this.scheduledDueDate = scheduledDueDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientStaffId() {
        return recipientStaffId;
    }

    public void setRecipientStaffId(Long recipientStaffId) {
        this.recipientStaffId = recipientStaffId;
    }

    public String getRecipientStaffName() {
        return recipientStaffName;
    }

    public void setRecipientStaffName(String recipientStaffName) {
        this.recipientStaffName = recipientStaffName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }

    public Long getCalibrationScheduleId() {
        return calibrationScheduleId;
    }

    public void setCalibrationScheduleId(Long calibrationScheduleId) {
        this.calibrationScheduleId = calibrationScheduleId;
    }

    public LocalDate getScheduledDueDate() {
        return scheduledDueDate;
    }

    public void setScheduledDueDate(LocalDate scheduledDueDate) {
        this.scheduledDueDate = scheduledDueDate;
    }
}
