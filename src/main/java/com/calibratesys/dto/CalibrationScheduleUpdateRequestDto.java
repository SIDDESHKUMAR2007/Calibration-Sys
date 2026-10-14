package com.calibratesys.dto;

import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CalibrationScheduleUpdateRequestDto {

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Schedule priority is required")
    private SchedulePriority priority;

    private ScheduleStatus status;

    @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
    private String remarks;

    public CalibrationScheduleUpdateRequestDto() {
    }

    public CalibrationScheduleUpdateRequestDto(LocalDate scheduledDate, LocalDate dueDate,
                                               SchedulePriority priority, ScheduleStatus status, String remarks) {
        this.scheduledDate = scheduledDate;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.remarks = remarks;
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

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
