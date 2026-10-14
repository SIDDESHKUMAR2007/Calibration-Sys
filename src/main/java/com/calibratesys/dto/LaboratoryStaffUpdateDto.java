package com.calibratesys.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for updating an existing Staff assignment in a Laboratory.
 */
public class LaboratoryStaffUpdateDto {

    @NotBlank(message = "Role in laboratory is required")
    private String roleInLab;

    private Boolean isPrimaryContact = false;

    private String status = "ACTIVE";

    public LaboratoryStaffUpdateDto() {
    }

    public LaboratoryStaffUpdateDto(String roleInLab, Boolean isPrimaryContact, String status) {
        this.roleInLab = roleInLab;
        this.isPrimaryContact = isPrimaryContact != null ? isPrimaryContact : false;
        this.status = status != null ? status : "ACTIVE";
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
