package com.calibratesys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a Laboratory.
 */
public class LaboratoryRequestDto {

    @NotNull(message = "College ID is required")
    private Long collegeId;

    @NotBlank(message = "Laboratory code is required")
    @Size(min = 2, max = 50, message = "Laboratory code must be between 2 and 50 characters")
    private String labCode;

    @NotBlank(message = "Laboratory name is required")
    @Size(min = 2, max = 150, message = "Laboratory name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    private String department;

    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    private String status;

    public LaboratoryRequestDto() {
    }

    public LaboratoryRequestDto(Long collegeId, String labCode, String name, String department, String location, String status) {
        this.collegeId = collegeId;
        this.labCode = labCode;
        this.name = name;
        this.department = department;
        this.location = location;
        this.status = status;
    }

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
