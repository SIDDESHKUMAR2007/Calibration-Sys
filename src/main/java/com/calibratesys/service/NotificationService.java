package com.calibratesys.service;

import com.calibratesys.dto.MarkAllReadResponseDto;
import com.calibratesys.dto.NotificationAlertSummaryDto;
import com.calibratesys.dto.NotificationResponseDto;
import com.calibratesys.dto.UnreadCountResponseDto;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.NotificationPriority;
import com.calibratesys.entity.enums.NotificationType;

import java.time.LocalDate;
import java.util.List;

public interface NotificationService {

    NotificationAlertSummaryDto generateCalibrationAlerts();

    List<NotificationResponseDto> getCurrentUserNotifications(NotificationType type,
                                                             NotificationPriority priority,
                                                             Boolean isRead,
                                                             Long equipmentId,
                                                             Long laboratoryId,
                                                             LocalDate fromDate,
                                                             LocalDate toDate);

    List<NotificationResponseDto> getCurrentUserUnreadNotifications();

    UnreadCountResponseDto getCurrentUserUnreadCount();

    NotificationResponseDto getNotificationById(Long id);

    NotificationResponseDto markNotificationAsRead(Long id);

    MarkAllReadResponseDto markAllNotificationsAsRead();

    List<Staff> resolveResponsibleStaff(Laboratory laboratory);
}
