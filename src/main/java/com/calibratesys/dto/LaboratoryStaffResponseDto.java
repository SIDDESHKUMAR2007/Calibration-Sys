package com.calibratesys.dto;

import java.time.LocalDateTime;

/**
 * Response representation for a Staff-Laboratory assignment record.
 * Designed to provide full traceability for operational workflows and future notification routing.
 */
public class LaboratoryStaffResponseDto {

    private Long id;
    private Long laboratoryId;
    private String laboratoryName;
    private String laboratoryCode;
    private Long staffId;
    private String staffEmployeeId;
    private String staffName;
    private String staffEmail;
    private String staffPhoneNumber;
    private String staffDepartment;
    private String staffDesignation;
    private String staffStatus;
    private String roleInLab;
    private boolean isPrimaryContact;
    private String assignmentStatus;
    private LocalDateTime assignedAt;

    public LaboratoryStaffResponseDto() {
    }

    public LaboratoryStaffResponseDto(Long id, Long laboratoryId, String laboratoryName, String laboratoryCode,
                                     Long staffId, String staffEmployeeId, String staffName,
                                     String staffEmail, String staffPhoneNumber, String staffDepartment,
                                     String staffDesignation, String staffStatus, String roleInLab,
                                     boolean isPrimaryContact, String assignmentStatus, LocalDateTime assignedAt) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.laboratoryCode = laboratoryCode;
        this.staffId = staffId;
        this.staffEmployeeId = staffEmployeeId;
        this.staffName = staffName;
        this.staffEmail = staffEmail;
        this.staffPhoneNumber = staffPhoneNumber;
        this.staffDepartment = staffDepartment;
        this.staffDesignation = staffDesignation;
        this.staffStatus = staffStatus;
        this.roleInLab = roleInLab;
        this.isPrimaryContact = isPrimaryContact;
        this.assignmentStatus = assignmentStatus;
        this.assignedAt = assignedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getLaboratoryCode() {
        return laboratoryCode;
    }

    public void setLaboratoryCode(String laboratoryCode) {
        this.laboratoryCode = laboratoryCode;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffEmployeeId() {
        return staffEmployeeId;
    }

    public void setStaffEmployeeId(String staffEmployeeId) {
        this.staffEmployeeId = staffEmployeeId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffEmail() {
        return staffEmail;
    }

    public void setStaffEmail(String staffEmail) {
        this.staffEmail = staffEmail;
    }

    public String getStaffPhoneNumber() {
        return staffPhoneNumber;
    }

    public void setStaffPhoneNumber(String staffPhoneNumber) {
        this.staffPhoneNumber = staffPhoneNumber;
    }

    public String getStaffDepartment() {
        return staffDepartment;
    }

    public void setStaffDepartment(String staffDepartment) {
        this.staffDepartment = staffDepartment;
    }

    public String getStaffDesignation() {
        return staffDesignation;
    }

    public void setStaffDesignation(String staffDesignation) {
        this.staffDesignation = staffDesignation;
    }

    public String getStaffStatus() {
        return staffStatus;
    }

    public void setStaffStatus(String staffStatus) {
        this.staffStatus = staffStatus;
    }

    public String getRoleInLab() {
        return roleInLab;
    }

    public void setRoleInLab(String roleInLab) {
        this.roleInLab = roleInLab;
    }

    public boolean isPrimaryContact() {
        return isPrimaryContact;
    }

    public void setPrimaryContact(boolean primaryContact) {
        isPrimaryContact = primaryContact;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
