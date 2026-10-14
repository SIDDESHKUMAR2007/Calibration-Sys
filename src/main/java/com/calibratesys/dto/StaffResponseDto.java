package com.calibratesys.dto;

import java.time.LocalDateTime;

/**
 * Response projection for Staff information.
 * Strictly excludes passwordHash and sensitive authentication details.
 */
public class StaffResponseDto {

    private Long id;
    private String staffId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String designation;
    private String department;
    private String systemRole;
    private String status;
    private int assignedLaboratoriesCount;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StaffResponseDto() {
    }

    public StaffResponseDto(Long id, String staffId, String firstName, String lastName,
                            String fullName, String email, String phoneNumber,
                            String designation, String department, String systemRole,
                            String status, int assignedLaboratoriesCount,
                            LocalDateTime lastLoginAt, LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this.id = id;
        this.staffId = staffId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.designation = designation;
        this.department = department;
        this.systemRole = systemRole;
        this.status = status;
        this.assignedLaboratoriesCount = assignedLaboratoriesCount;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAssignedLaboratoriesCount() {
        return assignedLaboratoriesCount;
    }

    public void setAssignedLaboratoriesCount(int assignedLaboratoriesCount) {
        this.assignedLaboratoriesCount = assignedLaboratoriesCount;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
