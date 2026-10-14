package com.calibratesys.dto;

import java.time.LocalDateTime;

/**
 * DTO for returning Laboratory representations without exposing JPA entity internals.
 */
public class LaboratoryResponseDto {

    private Long id;
    private Long collegeId;
    private String collegeCode;
    private String collegeName;
    private String labCode;
    private String name;
    private String department;
    private String location;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LaboratoryResponseDto() {
    }

    public LaboratoryResponseDto(Long id, Long collegeId, String collegeCode, String collegeName,
                                 String labCode, String name, String department, String location,
                                 String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.collegeId = collegeId;
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
        this.labCode = labCode;
        this.name = name;
        this.department = department;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
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
