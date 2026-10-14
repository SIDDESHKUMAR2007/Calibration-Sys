package com.calibratesys.dto;

import java.util.List;

public class DashboardResponseDto {

    private DashboardSummaryDto summary;
    private CalibrationStatusSummaryDto calibrationStatus;
    private EquipmentStatusSummaryDto equipmentStatus;
    private List<DashboardCalibrationItemDto> upcomingCalibrations;
    private List<DashboardCalibrationItemDto> dueCalibrations;
    private List<DashboardCalibrationItemDto> overdueCalibrations;
    private List<RecentCalibrationDto> recentCalibrations;
    private List<LaboratoryDashboardSummaryDto> laboratories;
    private List<NotificationResponseDto> recentNotifications;
    private Long unreadNotificationCount;

    public DashboardResponseDto() {
    }

    public DashboardResponseDto(DashboardSummaryDto summary, CalibrationStatusSummaryDto calibrationStatus,
                                EquipmentStatusSummaryDto equipmentStatus,
                                List<DashboardCalibrationItemDto> upcomingCalibrations,
                                List<DashboardCalibrationItemDto> dueCalibrations,
                                List<DashboardCalibrationItemDto> overdueCalibrations,
                                List<RecentCalibrationDto> recentCalibrations,
                                List<LaboratoryDashboardSummaryDto> laboratories,
                                List<NotificationResponseDto> recentNotifications,
                                Long unreadNotificationCount) {
        this.summary = summary;
        this.calibrationStatus = calibrationStatus;
        this.equipmentStatus = equipmentStatus;
        this.upcomingCalibrations = upcomingCalibrations;
        this.dueCalibrations = dueCalibrations;
        this.overdueCalibrations = overdueCalibrations;
        this.recentCalibrations = recentCalibrations;
        this.laboratories = laboratories;
        this.recentNotifications = recentNotifications;
        this.unreadNotificationCount = unreadNotificationCount;
    }

    public DashboardSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(DashboardSummaryDto summary) {
        this.summary = summary;
    }

    public CalibrationStatusSummaryDto getCalibrationStatus() {
        return calibrationStatus;
    }

    public void setCalibrationStatus(CalibrationStatusSummaryDto calibrationStatus) {
        this.calibrationStatus = calibrationStatus;
    }

    public EquipmentStatusSummaryDto getEquipmentStatus() {
        return equipmentStatus;
    }

    public void setEquipmentStatus(EquipmentStatusSummaryDto equipmentStatus) {
        this.equipmentStatus = equipmentStatus;
    }

    public List<DashboardCalibrationItemDto> getUpcomingCalibrations() {
        return upcomingCalibrations;
    }

    public void setUpcomingCalibrations(List<DashboardCalibrationItemDto> upcomingCalibrations) {
        this.upcomingCalibrations = upcomingCalibrations;
    }

    public List<DashboardCalibrationItemDto> getDueCalibrations() {
        return dueCalibrations;
    }

    public void setDueCalibrations(List<DashboardCalibrationItemDto> dueCalibrations) {
        this.dueCalibrations = dueCalibrations;
    }

    public List<DashboardCalibrationItemDto> getOverdueCalibrations() {
        return overdueCalibrations;
    }

    public void setOverdueCalibrations(List<DashboardCalibrationItemDto> overdueCalibrations) {
        this.overdueCalibrations = overdueCalibrations;
    }

    public List<RecentCalibrationDto> getRecentCalibrations() {
        return recentCalibrations;
    }

    public void setRecentCalibrations(List<RecentCalibrationDto> recentCalibrations) {
        this.recentCalibrations = recentCalibrations;
    }

    public List<LaboratoryDashboardSummaryDto> getLaboratories() {
        return laboratories;
    }

    public void setLaboratories(List<LaboratoryDashboardSummaryDto> laboratories) {
        this.laboratories = laboratories;
    }

    public List<NotificationResponseDto> getRecentNotifications() {
        return recentNotifications;
    }

    public void setRecentNotifications(List<NotificationResponseDto> recentNotifications) {
        this.recentNotifications = recentNotifications;
    }

    public Long getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void setUnreadNotificationCount(Long unreadNotificationCount) {
        this.unreadNotificationCount = unreadNotificationCount;
    }
}
