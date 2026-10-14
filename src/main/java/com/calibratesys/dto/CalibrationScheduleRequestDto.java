package com.calibratesys.dto;

import com.calibratesys.entity.enums.SchedulePriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CalibrationScheduleRequestDto {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Schedule priority is required")
    private SchedulePriority priority;

    @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
    private String remarks;

    public CalibrationScheduleRequestDto() {
    }

    public CalibrationScheduleRequestDto(Long equipmentId, LocalDate scheduledDate, LocalDate dueDate,
                                         SchedulePriority priority, String remarks) {
        this.equipmentId = equipmentId;
        this.scheduledDate = scheduledDate;
        this.dueDate = dueDate;
        this.priority = priority;
        this.remarks = remarks;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public SchedulePriority getPriority() {
        return priority;
    }

    public void setPriority(SchedulePriority priority) {
        this.priority = priority;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
