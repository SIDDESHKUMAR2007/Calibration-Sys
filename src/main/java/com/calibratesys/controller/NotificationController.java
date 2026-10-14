package com.calibratesys.controller;

import com.calibratesys.dto.MarkAllReadResponseDto;
import com.calibratesys.dto.NotificationAlertSummaryDto;
import com.calibratesys.dto.NotificationResponseDto;
import com.calibratesys.dto.UnreadCountResponseDto;
import com.calibratesys.entity.enums.NotificationPriority;
import com.calibratesys.entity.enums.NotificationType;
import com.calibratesys.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for In-App Notifications and Calibration Alert Management.
 * All notification retrieval, status updates, and counters are strictly scoped to the authenticated user.
 * Calibration alert generation is restricted to ADMIN role.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Retrieve notifications for the currently authenticated staff member with optional filters.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationPriority priority,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        List<NotificationResponseDto> notifications = notificationService.getCurrentUserNotifications(
                type, priority, isRead, equipmentId, laboratoryId, fromDate, toDate
        );
        return ResponseEntity.ok(notifications);
    }

    /**
     * Retrieve only unread notifications for the currently authenticated staff member.
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponseDto>> getUnreadNotifications() {
        return ResponseEntity.ok(notificationService.getCurrentUserUnreadNotifications());
    }

    /**
     * Retrieve the count of unread notifications for the currently authenticated staff member.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDto> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getCurrentUserUnreadCount());
    }

    /**
     * Retrieve a specific notification by ID (restricted to the recipient).
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    /**
     * Mark a specific notification as read (restricted to the recipient).
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markNotificationAsRead(id));
    }

    /**
     * Mark all unread notifications for the currently authenticated staff member as read.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllReadResponseDto> markAllAsRead() {
        return ResponseEntity.ok(notificationService.markAllNotificationsAsRead());
    }

    /**
     * Manually trigger calibration alert evaluation and generation (ADMIN only).
     */
    @PostMapping("/generate-calibration-alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationAlertSummaryDto> generateCalibrationAlerts() {
        NotificationAlertSummaryDto summary = notificationService.generateCalibrationAlerts();
        return ResponseEntity.ok(summary);
    }
}
