package com.calibratesys.entity;

import com.calibratesys.entity.enums.NotificationPriority;
import com.calibratesys.entity.enums.NotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_recipient", columnList = "recipient_staff_id"),
    @Index(name = "idx_notif_read", columnList = "recipient_staff_id, is_read"),
    @Index(name = "idx_notif_type", columnList = "notification_type"),
    @Index(name = "idx_notif_created", columnList = "created_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_staff_id", nullable = false)
    private Staff recipientStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratory_id")
    private Laboratory laboratory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private CalibrationSchedule calibrationSchedule;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Staff recipientStaff, Laboratory laboratory, Equipment equipment,
                        CalibrationSchedule calibrationSchedule, String title, String message,
                        NotificationType notificationType, NotificationPriority priority) {
        this.recipientStaff = recipientStaff;
        this.laboratory = laboratory;
        this.equipment = equipment;
        this.calibrationSchedule = calibrationSchedule;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.priority = priority != null ? priority : NotificationPriority.MEDIUM;
        this.isRead = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Staff getRecipientStaff() {
        return recipientStaff;
    }

    public void setRecipientStaff(Staff recipientStaff) {
        this.recipientStaff = recipientStaff;
    }

    public Laboratory getLaboratory() {
        return laboratory;
    }

    public void setLaboratory(Laboratory laboratory) {
        this.laboratory = laboratory;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public CalibrationSchedule getCalibrationSchedule() {
        return calibrationSchedule;
    }

    public void setCalibrationSchedule(CalibrationSchedule calibrationSchedule) {
        this.calibrationSchedule = calibrationSchedule;
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
}
