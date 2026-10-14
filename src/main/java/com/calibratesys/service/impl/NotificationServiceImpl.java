package com.calibratesys.service.impl;

import com.calibratesys.dto.MarkAllReadResponseDto;
import com.calibratesys.dto.NotificationAlertSummaryDto;
import com.calibratesys.dto.NotificationResponseDto;
import com.calibratesys.dto.UnreadCountResponseDto;
import com.calibratesys.entity.CalibrationSchedule;
import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.LaboratoryStaff;
import com.calibratesys.entity.Notification;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.NotificationPriority;
import com.calibratesys.entity.enums.NotificationType;
import com.calibratesys.entity.enums.ScheduleStatus;
import com.calibratesys.entity.enums.StaffLabRole;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.CalibrationScheduleRepository;
import com.calibratesys.repository.LaboratoryStaffRepository;
import com.calibratesys.repository.NotificationRepository;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import com.calibratesys.service.AuditService;
import com.calibratesys.service.CalibrationScheduleService;
import com.calibratesys.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final CalibrationScheduleRepository calibrationScheduleRepository;
    private final CalibrationScheduleService calibrationScheduleService;
    private final StaffRepository staffRepository;
    private final AuditService auditService;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   LaboratoryStaffRepository laboratoryStaffRepository,
                                   CalibrationScheduleRepository calibrationScheduleRepository,
                                   CalibrationScheduleService calibrationScheduleService,
                                   StaffRepository staffRepository,
                                   AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.calibrationScheduleRepository = calibrationScheduleRepository;
        this.calibrationScheduleService = calibrationScheduleService;
        this.staffRepository = staffRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public NotificationAlertSummaryDto generateCalibrationAlerts() {
        // Step 1: Refresh schedule status calculations based on current date
        calibrationScheduleService.refreshActiveScheduleStatuses();

        int upcomingCount = 0;
        int dueCount = 0;
        int overdueCount = 0;
        int createdCount = 0;
        int duplicateCount = 0;

        // Process UPCOMING Schedules
        List<CalibrationSchedule> upcomingSchedules = calibrationScheduleRepository.findByStatus(ScheduleStatus.UPCOMING);
        for (CalibrationSchedule schedule : upcomingSchedules) {
            upcomingCount++;
            Equipment equipment = schedule.getEquipment();
            Laboratory lab = equipment != null ? equipment.getLaboratory() : null;
            List<Staff> recipients = resolveResponsibleStaff(lab);

            for (Staff staff : recipients) {
                Long equipmentId = equipment != null ? equipment.getId() : null;
                if (notificationRepository.existsUnreadEquivalent(staff.getId(), equipmentId, schedule.getId(), NotificationType.CALIBRATION_UPCOMING)) {
                    duplicateCount++;
                } else {
                    Notification notification = new Notification(
                            staff,
                            lab,
                            equipment,
                            schedule,
                            "Calibration Upcoming",
                            "Equipment " + (equipment != null ? equipment.getName() : "Unknown") +
                                    " (" + (equipment != null ? equipment.getAssetTag() : "-") + ") in " +
                                    (lab != null ? lab.getName() : "Unknown Lab") +
                                    " is approaching its calibration due date on " + schedule.getDueDate() + ".",
                            NotificationType.CALIBRATION_UPCOMING,
                            NotificationPriority.MEDIUM
                    );
                    notificationRepository.save(notification);
                    createdCount++;
                }
            }
        }

        // Process DUE Schedules
        List<CalibrationSchedule> dueSchedules = calibrationScheduleRepository.findByStatus(ScheduleStatus.DUE);
        for (CalibrationSchedule schedule : dueSchedules) {
            dueCount++;
            Equipment equipment = schedule.getEquipment();
            Laboratory lab = equipment != null ? equipment.getLaboratory() : null;
            List<Staff> recipients = resolveResponsibleStaff(lab);

            for (Staff staff : recipients) {
                Long equipmentId = equipment != null ? equipment.getId() : null;
                if (notificationRepository.existsUnreadEquivalent(staff.getId(), equipmentId, schedule.getId(), NotificationType.CALIBRATION_DUE)) {
                    duplicateCount++;
                } else {
                    Notification notification = new Notification(
                            staff,
                            lab,
                            equipment,
                            schedule,
                            "Calibration Due",
                            "Equipment " + (equipment != null ? equipment.getName() : "Unknown") +
                                    " (" + (equipment != null ? equipment.getAssetTag() : "-") + ") in " +
                                    (lab != null ? lab.getName() : "Unknown Lab") +
                                    " is due for calibration on " + schedule.getDueDate() + ".",
                            NotificationType.CALIBRATION_DUE,
                            NotificationPriority.HIGH
                    );
                    notificationRepository.save(notification);
                    createdCount++;
                }
            }
        }

        // Process OVERDUE Schedules
        List<CalibrationSchedule> overdueSchedules = calibrationScheduleRepository.findByStatus(ScheduleStatus.OVERDUE);
        for (CalibrationSchedule schedule : overdueSchedules) {
            overdueCount++;
            Equipment equipment = schedule.getEquipment();
            Laboratory lab = equipment != null ? equipment.getLaboratory() : null;
            List<Staff> recipients = resolveResponsibleStaff(lab);

            for (Staff staff : recipients) {
                Long equipmentId = equipment != null ? equipment.getId() : null;
                if (notificationRepository.existsUnreadEquivalent(staff.getId(), equipmentId, schedule.getId(), NotificationType.CALIBRATION_OVERDUE)) {
                    duplicateCount++;
                } else {
                    Notification notification = new Notification(
                            staff,
                            lab,
                            equipment,
                            schedule,
                            "Calibration Overdue",
                            "Equipment " + (equipment != null ? equipment.getName() : "Unknown") +
                                    " (" + (equipment != null ? equipment.getAssetTag() : "-") + ") in " +
                                    (lab != null ? lab.getName() : "Unknown Lab") +
                                    " is overdue for calibration since " + schedule.getDueDate() + ".",
                            NotificationType.CALIBRATION_OVERDUE,
                            NotificationPriority.HIGH
                    );
                    notificationRepository.save(notification);
                    createdCount++;
                }
            }
        }

        NotificationAlertSummaryDto summary = new NotificationAlertSummaryDto(upcomingCount, dueCount, overdueCount, createdCount, duplicateCount);

        auditService.logEvent(
                AuditAction.ALERT_GENERATION_TRIGGERED,
                AuditEntityType.NOTIFICATION,
                null,
                "Calibration alert generation triggered: " + createdCount + " alerts created, " + duplicateCount + " duplicates skipped.",
                null,
                Map.of(
                        "upcomingCount", upcomingCount,
                        "dueCount", dueCount,
                        "overdueCount", overdueCount,
                        "createdCount", createdCount,
                        "duplicateCount", duplicateCount
                )
        );

        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getCurrentUserNotifications(NotificationType type,
                                                                   NotificationPriority priority,
                                                                   Boolean isRead,
                                                                   Long equipmentId,
                                                                   Long laboratoryId,
                                                                   LocalDate fromDate,
                                                                   LocalDate toDate) {
        Staff currentStaff = getAuthenticatedStaff();
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        return notificationRepository.findWithFilters(
                currentStaff.getId(), type, priority, isRead, equipmentId, laboratoryId, fromDateTime, toDateTime
        ).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getCurrentUserUnreadNotifications() {
        Staff currentStaff = getAuthenticatedStaff();
        return notificationRepository.findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(currentStaff.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponseDto getCurrentUserUnreadCount() {
        Staff currentStaff = getAuthenticatedStaff();
        long unreadCount = notificationRepository.countByRecipientStaffIdAndIsReadFalse(currentStaff.getId());
        return new UnreadCountResponseDto(unreadCount);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationById(Long id) {
        Staff currentStaff = getAuthenticatedStaff();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Notification not found with id: " + id, HttpStatus.NOT_FOUND));

        if (!notification.getRecipientStaff().getId().equals(currentStaff.getId())) {
            throw new ApiException("Access denied: You do not have permission to access this notification", HttpStatus.FORBIDDEN);
        }

        return mapToDto(notification);
    }

    @Override
    @Transactional
    public NotificationResponseDto markNotificationAsRead(Long id) {
        Staff currentStaff = getAuthenticatedStaff();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException("Notification not found with id: " + id, HttpStatus.NOT_FOUND));

        if (!notification.getRecipientStaff().getId().equals(currentStaff.getId())) {
            throw new ApiException("Access denied: You do not have permission to modify this notification", HttpStatus.FORBIDDEN);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);

            auditService.logEvent(
                    AuditAction.NOTIFICATION_MARKED_READ,
                    AuditEntityType.NOTIFICATION,
                    notification.getId(),
                    "Notification #" + notification.getId() + " marked as read.",
                    Map.of("isRead", false),
                    Map.of("isRead", true, "readAt", notification.getReadAt().toString())
            );
        }

        return mapToDto(notification);
    }

    @Override
    @Transactional
    public MarkAllReadResponseDto markAllNotificationsAsRead() {
        Staff currentStaff = getAuthenticatedStaff();
        int count = notificationRepository.markAllAsReadForRecipient(currentStaff.getId(), LocalDateTime.now());
        if (count > 0) {
            auditService.logEvent(
                    AuditAction.NOTIFICATION_MARKED_READ,
                    AuditEntityType.NOTIFICATION,
                    null,
                    "Marked all (" + count + ") notifications as read for staff #" + currentStaff.getId() + " (" + currentStaff.getEmail() + ").",
                    Map.of("unreadCountBefore", count),
                    Map.of("markedReadCount", count)
            );
        }
        return new MarkAllReadResponseDto(count, "All notifications marked as read");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Staff> resolveResponsibleStaff(Laboratory laboratory) {
        if (laboratory == null || laboratory.getId() == null) {
            return Collections.emptyList();
        }

        // Active laboratory assignments for active staff
        List<LaboratoryStaff> activeAssignments = laboratoryStaffRepository.findActiveAssignmentsByLaboratoryId(laboratory.getId());
        if (activeAssignments.isEmpty()) {
            return Collections.emptyList();
        }

        // Case A — Active Primary Contact Exists
        List<Staff> primaryContacts = activeAssignments.stream()
                .filter(LaboratoryStaff::isPrimaryContact)
                .map(LaboratoryStaff::getStaff)
                .distinct()
                .toList();

        if (!primaryContacts.isEmpty()) {
            return primaryContacts;
        }

        // Case B — No Primary Contact: Prioritize calibration coordinators and lab technicians
        List<Staff> specializedStaff = activeAssignments.stream()
                .filter(a -> a.getRoleInLab() == StaffLabRole.CALIBRATION_COORDINATOR ||
                             a.getRoleInLab() == StaffLabRole.LAB_TECHNICIAN)
                .map(LaboratoryStaff::getStaff)
                .distinct()
                .toList();

        if (!specializedStaff.isEmpty()) {
            return specializedStaff;
        }

        // Case C — Fallback to any other active laboratory staff
        return activeAssignments.stream()
                .map(LaboratoryStaff::getStaff)
                .distinct()
                .toList();
    }

    private Staff getAuthenticatedStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ApiException("User is not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String username;
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = authentication.getName();
        }

        return staffRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException("Staff user not found", HttpStatus.NOT_FOUND));
    }

    private NotificationResponseDto mapToDto(Notification n) {
        Long labId = n.getLaboratory() != null ? n.getLaboratory().getId() : null;
        String labName = n.getLaboratory() != null ? n.getLaboratory().getName() : null;
        Long eqId = n.getEquipment() != null ? n.getEquipment().getId() : null;
        String eqName = n.getEquipment() != null ? n.getEquipment().getName() : null;
        String eqCode = n.getEquipment() != null ? n.getEquipment().getAssetTag() : null;
        Long schedId = n.getCalibrationSchedule() != null ? n.getCalibrationSchedule().getId() : null;
        LocalDate dueDate = n.getCalibrationSchedule() != null ? n.getCalibrationSchedule().getDueDate() : null;

        return new NotificationResponseDto(
                n.getId(),
                n.getRecipientStaff().getId(),
                n.getRecipientStaff().getFullName(),
                n.getTitle(),
                n.getMessage(),
                n.getNotificationType(),
                n.getPriority(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt(),
                labId,
                labName,
                eqId,
                eqName,
                eqCode,
                schedId,
                dueDate
        );
    }
}
