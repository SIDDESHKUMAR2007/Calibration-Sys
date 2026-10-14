package com.calibratesys.entity;

import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calibration_schedules", indexes = {
    @Index(name = "idx_calib_sched_equip", columnList = "equipment_id"),
    @Index(name = "idx_calib_sched_due", columnList = "due_date"),
    @Index(name = "idx_calib_sched_status", columnList = "status")
})
public class CalibrationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchedulePriority priority = SchedulePriority.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @OneToMany(mappedBy = "calibrationSchedule", fetch = FetchType.LAZY)
    private List<CalibrationRecord> calibrationRecords = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CalibrationSchedule() {
    }

    public CalibrationSchedule(Equipment equipment, LocalDate scheduledDate, LocalDate dueDate,
                               ScheduleStatus status, SchedulePriority priority, String remarks) {
        this.equipment = equipment;
        this.scheduledDate = scheduledDate;
        this.dueDate = dueDate;
        this.status = status != null ? status : ScheduleStatus.SCHEDULED;
        this.priority = priority != null ? priority : SchedulePriority.MEDIUM;
        this.remarks = remarks;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
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

    public List<CalibrationRecord> getCalibrationRecords() {
        return calibrationRecords;
    }

    public void setCalibrationRecords(List<CalibrationRecord> calibrationRecords) {
        this.calibrationRecords = calibrationRecords;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
