package com.calibratesys.dto;

public class DashboardSummaryDto {

    private Long totalColleges;
    private Long totalLaboratories;
    private Long activeLaboratories;
    private Long totalEquipment;
    private Long operationalEquipment;
    private Long equipmentUnderMaintenance;
    private Long equipmentCalibrationDue;
    private Long equipmentOutOfCalibration;
    private Long equipmentDecommissioned;
    private Long upcomingSchedules;
    private Long dueSchedules;
    private Long overdueSchedules;
    private Long completedSchedules;
    private Long totalCalibrationRecords;
    private Long recordsWithDocuments;
    private Long unreadNotificationsCount;

    public DashboardSummaryDto() {
    }

    public DashboardSummaryDto(Long totalColleges, Long totalLaboratories, Long activeLaboratories,
                               Long totalEquipment, Long operationalEquipment, Long equipmentUnderMaintenance,
                               Long equipmentCalibrationDue, Long equipmentOutOfCalibration, Long equipmentDecommissioned,
                               Long upcomingSchedules, Long dueSchedules, Long overdueSchedules, Long completedSchedules,
                               Long totalCalibrationRecords, Long recordsWithDocuments, Long unreadNotificationsCount) {
        this.totalColleges = totalColleges;
        this.totalLaboratories = totalLaboratories;
        this.activeLaboratories = activeLaboratories;
        this.totalEquipment = totalEquipment;
        this.operationalEquipment = operationalEquipment;
        this.equipmentUnderMaintenance = equipmentUnderMaintenance;
        this.equipmentCalibrationDue = equipmentCalibrationDue;
        this.equipmentOutOfCalibration = equipmentOutOfCalibration;
        this.equipmentDecommissioned = equipmentDecommissioned;
        this.upcomingSchedules = upcomingSchedules;
        this.dueSchedules = dueSchedules;
        this.overdueSchedules = overdueSchedules;
        this.completedSchedules = completedSchedules;
        this.totalCalibrationRecords = totalCalibrationRecords;
        this.recordsWithDocuments = recordsWithDocuments;
        this.unreadNotificationsCount = unreadNotificationsCount;
    }

    public Long getTotalColleges() {
        return totalColleges;
    }

    public void setTotalColleges(Long totalColleges) {
        this.totalColleges = totalColleges;
    }

    public Long getTotalLaboratories() {
        return totalLaboratories;
    }

    public void setTotalLaboratories(Long totalLaboratories) {
        this.totalLaboratories = totalLaboratories;
    }

    public Long getActiveLaboratories() {
        return activeLaboratories;
    }

    public void setActiveLaboratories(Long activeLaboratories) {
        this.activeLaboratories = activeLaboratories;
    }

    public Long getTotalEquipment() {
        return totalEquipment;
    }

    public void setTotalEquipment(Long totalEquipment) {
        this.totalEquipment = totalEquipment;
    }

    public Long getOperationalEquipment() {
        return operationalEquipment;
    }

    public void setOperationalEquipment(Long operationalEquipment) {
        this.operationalEquipment = operationalEquipment;
    }

    public Long getEquipmentUnderMaintenance() {
        return equipmentUnderMaintenance;
    }

    public void setEquipmentUnderMaintenance(Long equipmentUnderMaintenance) {
        this.equipmentUnderMaintenance = equipmentUnderMaintenance;
    }

    public Long getEquipmentCalibrationDue() {
        return equipmentCalibrationDue;
    }

    public void setEquipmentCalibrationDue(Long equipmentCalibrationDue) {
        this.equipmentCalibrationDue = equipmentCalibrationDue;
    }

    public Long getEquipmentOutOfCalibration() {
        return equipmentOutOfCalibration;
    }

    public void setEquipmentOutOfCalibration(Long equipmentOutOfCalibration) {
        this.equipmentOutOfCalibration = equipmentOutOfCalibration;
    }

    public Long getEquipmentDecommissioned() {
        return equipmentDecommissioned;
    }

    public void setEquipmentDecommissioned(Long equipmentDecommissioned) {
        this.equipmentDecommissioned = equipmentDecommissioned;
    }

    public Long getUpcomingSchedules() {
        return upcomingSchedules;
    }

    public void setUpcomingSchedules(Long upcomingSchedules) {
        this.upcomingSchedules = upcomingSchedules;
    }

    public Long getDueSchedules() {
        return dueSchedules;
    }

    public void setDueSchedules(Long dueSchedules) {
        this.dueSchedules = dueSchedules;
    }

    public Long getOverdueSchedules() {
        return overdueSchedules;
    }

    public void setOverdueSchedules(Long overdueSchedules) {
        this.overdueSchedules = overdueSchedules;
    }

    public Long getCompletedSchedules() {
        return completedSchedules;
    }

    public void setCompletedSchedules(Long completedSchedules) {
        this.completedSchedules = completedSchedules;
    }

    public Long getTotalCalibrationRecords() {
        return totalCalibrationRecords;
    }

    public void setTotalCalibrationRecords(Long totalCalibrationRecords) {
        this.totalCalibrationRecords = totalCalibrationRecords;
    }

    public Long getRecordsWithDocuments() {
        return recordsWithDocuments;
    }

    public void setRecordsWithDocuments(Long recordsWithDocuments) {
        this.recordsWithDocuments = recordsWithDocuments;
    }

    public Long getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(Long unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }
}
