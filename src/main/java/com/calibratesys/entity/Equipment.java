package com.calibratesys.entity;

import com.calibratesys.entity.enums.EquipmentStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "equipment", indexes = {
    @Index(name = "idx_equipment_lab", columnList = "laboratory_id"),
    @Index(name = "idx_equipment_asset", columnList = "asset_tag", unique = true),
    @Index(name = "idx_equipment_status", columnList = "status")
})
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @Column(name = "asset_tag", nullable = false, unique = true, length = 100)
    private String assetTag;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "model_number", length = 100)
    private String modelNumber;

    @Column(length = 100)
    private String manufacturer;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "calibration_frequency_days", nullable = false)
    private Integer calibrationFrequencyDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentStatus status = EquipmentStatus.OPERATIONAL;

    @OneToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private List<CalibrationSchedule> calibrationSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private List<CalibrationRecord> calibrationRecords = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private List<CalibrationDocument> calibrationDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Equipment() {
    }

    public Equipment(Laboratory laboratory, String assetTag, String serialNumber, String name,
                     String modelNumber, String manufacturer, LocalDate purchaseDate,
                     LocalDate installationDate, Integer calibrationFrequencyDays, EquipmentStatus status) {
        this.laboratory = laboratory;
        this.assetTag = assetTag;
        this.serialNumber = serialNumber;
        this.name = name;
        this.modelNumber = modelNumber;
        this.manufacturer = manufacturer;
        this.purchaseDate = purchaseDate;
        this.installationDate = installationDate;
        this.calibrationFrequencyDays = calibrationFrequencyDays;
        this.status = status != null ? status : EquipmentStatus.OPERATIONAL;
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

    public Laboratory getLaboratory() {
        return laboratory;
    }

    public void setLaboratory(Laboratory laboratory) {
        this.laboratory = laboratory;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(LocalDate installationDate) {
        this.installationDate = installationDate;
    }

    public Integer getCalibrationFrequencyDays() {
        return calibrationFrequencyDays;
    }

    public void setCalibrationFrequencyDays(Integer calibrationFrequencyDays) {
        this.calibrationFrequencyDays = calibrationFrequencyDays;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public List<CalibrationSchedule> getCalibrationSchedules() {
        return calibrationSchedules;
    }

    public void setCalibrationSchedules(List<CalibrationSchedule> calibrationSchedules) {
        this.calibrationSchedules = calibrationSchedules;
    }

    public List<CalibrationRecord> getCalibrationRecords() {
        return calibrationRecords;
    }

    public void setCalibrationRecords(List<CalibrationRecord> calibrationRecords) {
        this.calibrationRecords = calibrationRecords;
    }

    public List<CalibrationDocument> getCalibrationDocuments() {
        return calibrationDocuments;
    }

    public void setCalibrationDocuments(List<CalibrationDocument> calibrationDocuments) {
        this.calibrationDocuments = calibrationDocuments;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
