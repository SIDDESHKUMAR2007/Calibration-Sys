package com.calibratesys.entity;

import com.calibratesys.entity.enums.StaffLabRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "laboratory_staff",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_lab_staff", columnNames = {"laboratory_id", "staff_id"})
    },
    indexes = {
        @Index(name = "idx_lab_staff_lab", columnList = "laboratory_id"),
        @Index(name = "idx_lab_staff_staff", columnList = "staff_id"),
        @Index(name = "idx_lab_staff_role", columnList = "role_in_lab")
    }
)
public class LaboratoryStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_lab", nullable = false, length = 50)
    private StaffLabRole roleInLab = StaffLabRole.STAFF;

    @Column(name = "is_primary_contact", nullable = false)
    private boolean isPrimaryContact = false;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public LaboratoryStaff() {
    }

    public LaboratoryStaff(Laboratory laboratory, Staff staff, StaffLabRole roleInLab, boolean isPrimaryContact) {
        this.laboratory = laboratory;
        this.staff = staff;
        this.roleInLab = roleInLab != null ? roleInLab : StaffLabRole.STAFF;
        this.isPrimaryContact = isPrimaryContact;
        this.status = "ACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }
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

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public StaffLabRole getRoleInLab() {
        return roleInLab;
    }

    public void setRoleInLab(StaffLabRole roleInLab) {
        this.roleInLab = roleInLab;
    }

    public boolean isPrimaryContact() {
        return isPrimaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        isPrimaryContact = primaryContact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
