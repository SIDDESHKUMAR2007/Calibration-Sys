package com.calibratesys.entity;

import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calibration_records", indexes = {
    @Index(name = "idx_calib_rec_equip", columnList = "equipment_id"),
    @Index(name = "idx_calib_rec_date", columnList = "calibration_date"),
    @Index(name = "idx_calib_rec_result", columnList = "result")
})
public class CalibrationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private CalibrationSchedule calibrationSchedule;

    @Column(name = "calibration_date", nullable = false)
    private LocalDate calibrationDate;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "calibrated_by_type", nullable = false, length = 30)
    private CalibratedByType calibratedByType = CalibratedByType.INTERNAL;

    @Column(name = "calibrator_name", nullable = false, length = 150)
    private String calibratorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalibrationResult result = CalibrationResult.PASSED;

    @Column(name = "uncertainty_measurement", length = 100)
    private String uncertaintyMeasurement;

    @Column(name = "standards_used", length = 255)
    private String standardsUsed;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "calibrationRecord", fetch = FetchType.LAZY)
    private List<CalibrationDocument> calibrationDocuments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CalibrationRecord() {
    }

    public CalibrationRecord(Equipment equipment, CalibrationSchedule calibrationSchedule,
                             LocalDate calibrationDate, LocalDate nextDueDate,
                             CalibratedByType calibratedByType, String calibratorName,
                             CalibrationResult result, String uncertaintyMeasurement,
                             String standardsUsed, String notes) {
        this.equipment = equipment;
        this.calibrationSchedule = calibrationSchedule;
        this.calibrationDate = calibrationDate;
        this.nextDueDate = nextDueDate;
        this.calibratedByType = calibratedByType != null ? calibratedByType : CalibratedByType.INTERNAL;
        this.calibratorName = calibratorName;
        this.result = result != null ? result : CalibrationResult.PASSED;
        this.uncertaintyMeasurement = uncertaintyMeasurement;
        this.standardsUsed = standardsUsed;
        this.notes = notes;
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

    public CalibrationSchedule getCalibrationSchedule() {
        return calibrationSchedule;
    }

    public void setCalibrationSchedule(CalibrationSchedule calibrationSchedule) {
        this.calibrationSchedule = calibrationSchedule;
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

    public CalibrationResult getResult() {
        return result;
    }

    public void setResult(CalibrationResult result) {
        this.result = result;
    }

    public String getUncertaintyMeasurement() {
        return uncertaintyMeasurement;
    }

    public void setUncertaintyMeasurement(String uncertaintyMeasurement) {
        this.uncertaintyMeasurement = uncertaintyMeasurement;
    }

    public String getStandardsUsed() {
        return standardsUsed;
    }

    public void setStandardsUsed(String standardsUsed) {
        this.standardsUsed = standardsUsed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<CalibrationDocument> getCalibrationDocuments() {
        return calibrationDocuments;
    }

    public void setCalibrationDocuments(List<CalibrationDocument> calibrationDocuments) {
        this.calibrationDocuments = calibrationDocuments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
