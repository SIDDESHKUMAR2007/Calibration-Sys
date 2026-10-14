package com.calibratesys.entity;

import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "staff", indexes = {
    @Index(name = "idx_staff_email", columnList = "email", unique = true),
    @Index(name = "idx_staff_id", columnList = "staff_id", unique = true),
    @Index(name = "idx_staff_role", columnList = "system_role"),
    @Index(name = "idx_staff_status", columnList = "status")
})
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false, unique = true, length = 50)
    private String staffId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 25)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(nullable = false, length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, length = 30)
    private SystemRole systemRole = SystemRole.LAB_STAFF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffStatus status = StaffStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "staff", fetch = FetchType.LAZY)
    private List<LaboratoryStaff> laboratoryAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "recipientStaff", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Staff() {
    }

    public Staff(String staffId, String firstName, String lastName, String email,
                 String passwordHash, String phoneNumber, String designation,
                 String department, SystemRole systemRole, StaffStatus status) {
        this.staffId = staffId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.designation = designation;
        this.department = department;
        this.systemRole = systemRole != null ? systemRole : SystemRole.LAB_STAFF;
        this.status = status != null ? status : StaffStatus.ACTIVE;
    }

    public Staff(String staffId, String firstName, String lastName, String email,
                 String phoneNumber, String designation, String department, StaffStatus status) {
        this(staffId, firstName, lastName, email, "", phoneNumber, designation, department, SystemRole.LAB_STAFF, status);
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

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public SystemRole getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(SystemRole systemRole) {
        this.systemRole = systemRole;
    }

    public StaffStatus getStatus() {
        return status;
    }

    public void setStatus(StaffStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public List<LaboratoryStaff> getLaboratoryAssignments() {
        return laboratoryAssignments;
    }

    public void setLaboratoryAssignments(List<LaboratoryStaff> laboratoryAssignments) {
        this.laboratoryAssignments = laboratoryAssignments;
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

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
