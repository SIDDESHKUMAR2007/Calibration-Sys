package com.calibratesys.dto;

public class EquipmentStatusSummaryDto {

    private Long operational;
    private Long calibrationDue;
    private Long outOfCalibration;
    private Long underMaintenance;
    private Long decommissioned;
    private Long total;

    public EquipmentStatusSummaryDto() {
    }

    public EquipmentStatusSummaryDto(Long operational, Long calibrationDue, Long outOfCalibration,
                                     Long underMaintenance, Long decommissioned, Long total) {
        this.operational = operational;
        this.calibrationDue = calibrationDue;
        this.outOfCalibration = outOfCalibration;
        this.underMaintenance = underMaintenance;
        this.decommissioned = decommissioned;
        this.total = total;
    }

    public Long getOperational() {
        return operational;
    }

    public void setOperational(Long operational) {
        this.operational = operational;
    }

    public Long getCalibrationDue() {
        return calibrationDue;
    }

    public void setCalibrationDue(Long calibrationDue) {
        this.calibrationDue = calibrationDue;
    }

    public Long getOutOfCalibration() {
        return outOfCalibration;
    }

    public void setOutOfCalibration(Long outOfCalibration) {
        this.outOfCalibration = outOfCalibration;
    }

    public Long getUnderMaintenance() {
        return underMaintenance;
    }

    public void setUnderMaintenance(Long underMaintenance) {
        this.underMaintenance = underMaintenance;
    }

    public Long getDecommissioned() {
        return decommissioned;
    }

    public void setDecommissioned(Long decommissioned) {
        this.decommissioned = decommissioned;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
