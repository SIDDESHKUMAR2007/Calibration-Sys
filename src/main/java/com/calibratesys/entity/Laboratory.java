package com.calibratesys.entity;

import com.calibratesys.entity.enums.LabStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "laboratories", indexes = {
    @Index(name = "idx_labs_college", columnList = "college_id"),
    @Index(name = "idx_labs_code", columnList = "lab_code", unique = true),
    @Index(name = "idx_labs_status", columnList = "status")
})
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @Column(name = "lab_code", nullable = false, unique = true, length = 50)
    private String labCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabStatus status = LabStatus.ACTIVE;

    @OneToMany(mappedBy = "laboratory", fetch = FetchType.LAZY)
    private List<LaboratoryStaff> staffAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "laboratory", fetch = FetchType.LAZY)
    private List<Equipment> equipmentList = new ArrayList<>();

    @OneToMany(mappedBy = "laboratory", fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Laboratory() {
    }

    public Laboratory(College college, String labCode, String name, String department, String location, LabStatus status) {
        this.college = college;
        this.labCode = labCode;
        this.name = name;
        this.department = department;
        this.location = location;
        this.status = status != null ? status : LabStatus.ACTIVE;
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

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
    }

    public String getLabCode() {
        return labCode;
    }

    public void setLabCode(String labCode) {
        this.labCode = labCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LabStatus getStatus() {
        return status;
    }

    public void setStatus(LabStatus status) {
        this.status = status;
    }

    public List<LaboratoryStaff> getStaffAssignments() {
        return staffAssignments;
    }

    public void setStaffAssignments(List<LaboratoryStaff> staffAssignments) {
        this.staffAssignments = staffAssignments;
    }

    public List<Equipment> getEquipmentList() {
        return equipmentList;
    }

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
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
