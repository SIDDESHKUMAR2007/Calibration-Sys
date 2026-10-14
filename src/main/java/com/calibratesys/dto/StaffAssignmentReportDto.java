package com.calibratesys.dto;

import com.calibratesys.entity.enums.StaffLabRole;

public class StaffAssignmentReportDto {

    private Long assignmentId;
    private String staffId;
    private Long staffInternalId;
    private String staffName;
    private String email;
    private String designation;
    private Long laboratoryId;
    private String laboratoryName;
    private String laboratoryCode;
    private StaffLabRole roleInLab;
    private boolean isPrimaryContact;
    private String assignmentStatus;

    public StaffAssignmentReportDto() {
    }

    public StaffAssignmentReportDto(Long assignmentId, String staffId, Long staffInternalId,
                                    String staffName, String email, String designation,
                                    Long laboratoryId, String laboratoryName, String laboratoryCode,
                                    StaffLabRole roleInLab, boolean isPrimaryContact,
                                    String assignmentStatus) {
        this.assignmentId = assignmentId;
        this.staffId = staffId;
        this.staffInternalId = staffInternalId;
        this.staffName = staffName;
        this.email = email;
        this.designation = designation;
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.laboratoryCode = laboratoryCode;
        this.roleInLab = roleInLab;
        this.isPrimaryContact = isPrimaryContact;
        this.assignmentStatus = assignmentStatus;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public Long getStaffInternalId() {
        return staffInternalId;
    }

    public void setStaffInternalId(Long staffInternalId) {
        this.staffInternalId = staffInternalId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
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

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }
}
