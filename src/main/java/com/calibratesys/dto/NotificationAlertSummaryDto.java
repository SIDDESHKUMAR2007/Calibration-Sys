package com.calibratesys.dto;

public class NotificationAlertSummaryDto {

    private int upcomingSchedulesProcessed;
    private int dueSchedulesProcessed;
    private int overdueSchedulesProcessed;
    private int notificationsCreated;
    private int notificationsSkippedAsDuplicates;

    public NotificationAlertSummaryDto() {
    }

    public NotificationAlertSummaryDto(int upcomingSchedulesProcessed, int dueSchedulesProcessed,
                                       int overdueSchedulesProcessed, int notificationsCreated,
                                       int notificationsSkippedAsDuplicates) {
        this.upcomingSchedulesProcessed = upcomingSchedulesProcessed;
        this.dueSchedulesProcessed = dueSchedulesProcessed;
        this.overdueSchedulesProcessed = overdueSchedulesProcessed;
        this.notificationsCreated = notificationsCreated;
        this.notificationsSkippedAsDuplicates = notificationsSkippedAsDuplicates;
    }

    public int getUpcomingSchedulesProcessed() {
        return upcomingSchedulesProcessed;
    }

    public void setUpcomingSchedulesProcessed(int upcomingSchedulesProcessed) {
        this.upcomingSchedulesProcessed = upcomingSchedulesProcessed;
    }

    public int getDueSchedulesProcessed() {
        return dueSchedulesProcessed;
    }

    public void setDueSchedulesProcessed(int dueSchedulesProcessed) {
        this.dueSchedulesProcessed = dueSchedulesProcessed;
    }

    public int getOverdueSchedulesProcessed() {
        return overdueSchedulesProcessed;
    }

    public void setOverdueSchedulesProcessed(int overdueSchedulesProcessed) {
        this.overdueSchedulesProcessed = overdueSchedulesProcessed;
    }

    public int getNotificationsCreated() {
        return notificationsCreated;
    }

    public void setNotificationsCreated(int notificationsCreated) {
        this.notificationsCreated = notificationsCreated;
    }

    public int getNotificationsSkippedAsDuplicates() {
        return notificationsSkippedAsDuplicates;
    }

    public void setNotificationsSkippedAsDuplicates(int notificationsSkippedAsDuplicates) {
        this.notificationsSkippedAsDuplicates = notificationsSkippedAsDuplicates;
    }
}
