package com.calibratesys.dto;

import com.calibratesys.entity.enums.LabStatus;

public class LaboratoryReportDto {

    private Long laboratoryId;
    private String laboratoryName;
    private String laboratoryCode;
    private Long collegeId;
    private String collegeName;
    private String department;
    private String location;
    private LabStatus laboratoryStatus;
    private long totalEquipmentCount;
    private long activeStaffCount;
    private long calibrationAttentionCount;

    public LaboratoryReportDto() {
    }

    public LaboratoryReportDto(Long laboratoryId, String laboratoryName, String laboratoryCode,
                               Long collegeId, String collegeName, String department, String location,
                               LabStatus laboratoryStatus, long totalEquipmentCount,
                               long activeStaffCount, long calibrationAttentionCount) {
        this.laboratoryId = laboratoryId;
        this.laboratoryName = laboratoryName;
        this.laboratoryCode = laboratoryCode;
        this.collegeId = collegeId;
        this.collegeName = collegeName;
        this.department = department;
        this.location = location;
        this.laboratoryStatus = laboratoryStatus;
        this.totalEquipmentCount = totalEquipmentCount;
        this.activeStaffCount = activeStaffCount;
        this.calibrationAttentionCount = calibrationAttentionCount;
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

    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
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

    public LabStatus getLaboratoryStatus() {
        return laboratoryStatus;
    }

    public void setLaboratoryStatus(LabStatus laboratoryStatus) {
        this.laboratoryStatus = laboratoryStatus;
    }

    public long getTotalEquipmentCount() {
        return totalEquipmentCount;
    }

    public void setTotalEquipmentCount(long totalEquipmentCount) {
        this.totalEquipmentCount = totalEquipmentCount;
    }

    public long getActiveStaffCount() {
        return activeStaffCount;
    }

    public void setActiveStaffCount(long activeStaffCount) {
        this.activeStaffCount = activeStaffCount;
    }

    public long getCalibrationAttentionCount() {
        return calibrationAttentionCount;
    }

    public void setCalibrationAttentionCount(long calibrationAttentionCount) {
        this.calibrationAttentionCount = calibrationAttentionCount;
    }
}
