package com.calibratesys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for assigning a Staff member to a Laboratory.
 */
public class LaboratoryStaffRequestDto {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @NotBlank(message = "Role in laboratory is required")
    private String roleInLab;

    private Boolean isPrimaryContact = false;

    private String status = "ACTIVE";

    public LaboratoryStaffRequestDto() {
    }

    public LaboratoryStaffRequestDto(Long staffId, String roleInLab, Boolean isPrimaryContact, String status) {
        this.staffId = staffId;
        this.roleInLab = roleInLab;
        this.isPrimaryContact = isPrimaryContact != null ? isPrimaryContact : false;
        this.status = status != null ? status : "ACTIVE";
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getRoleInLab() {
        return roleInLab;
    }

    public void setRoleInLab(String roleInLab) {
        this.roleInLab = roleInLab;
    }

    public Boolean getIsPrimaryContact() {
        return isPrimaryContact;
    }

    public void setIsPrimaryContact(Boolean primaryContact) {
        isPrimaryContact = primaryContact != null ? primaryContact : false;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "ACTIVE";
    }
}
