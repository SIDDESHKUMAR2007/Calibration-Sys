package com.calibratesys.dto;

public class CalibrationStatusSummaryDto {

    private Long scheduled;
    private Long upcoming;
    private Long due;
    private Long overdue;
    private Long completed;
    private Long cancelled;
    private Long total;

    public CalibrationStatusSummaryDto() {
    }

    public CalibrationStatusSummaryDto(Long scheduled, Long upcoming, Long due,
                                       Long overdue, Long completed, Long cancelled, Long total) {
        this.scheduled = scheduled;
        this.upcoming = upcoming;
        this.due = due;
        this.overdue = overdue;
        this.completed = completed;
        this.cancelled = cancelled;
        this.total = total;
    }

    public Long getScheduled() {
        return scheduled;
    }

    public void setScheduled(Long scheduled) {
        this.scheduled = scheduled;
    }

    public Long getUpcoming() {
        return upcoming;
    }

    public void setUpcoming(Long upcoming) {
        this.upcoming = upcoming;
    }

    public Long getDue() {
        return due;
    }

    public void setDue(Long due) {
        this.due = due;
    }

    public Long getOverdue() {
        return overdue;
    }

    public void setOverdue(Long overdue) {
        this.overdue = overdue;
    }

    public Long getCompleted() {
        return completed;
    }

    public void setCompleted(Long completed) {
        this.completed = completed;
    }

    public Long getCancelled() {
        return cancelled;
    }

    public void setCancelled(Long cancelled) {
        this.cancelled = cancelled;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
