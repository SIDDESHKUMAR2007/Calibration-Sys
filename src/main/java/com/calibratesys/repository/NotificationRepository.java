package com.calibratesys.repository;

import com.calibratesys.entity.Notification;
import com.calibratesys.entity.enums.NotificationPriority;
import com.calibratesys.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientStaffIdOrderByCreatedAtDesc(Long recipientStaffId);

    List<Notification> findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientStaffId);

    long countByRecipientStaffIdAndIsReadFalse(Long recipientStaffId);

    Optional<Notification> findByIdAndRecipientStaffId(Long id, Long recipientStaffId);

    List<Notification> findByEquipmentId(Long equipmentId);

    List<Notification> findByLaboratoryId(Long laboratoryId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE " +
           "n.recipientStaff.id = :recipientStaffId AND " +
           "(:scheduleId IS NULL OR (n.calibrationSchedule IS NOT NULL AND n.calibrationSchedule.id = :scheduleId)) AND " +
           "(:equipmentId IS NULL OR (n.equipment IS NOT NULL AND n.equipment.id = :equipmentId)) AND " +
           "n.notificationType = :notificationType AND " +
           "n.isRead = false")
    boolean existsUnreadEquivalent(
            @Param("recipientStaffId") Long recipientStaffId,
            @Param("equipmentId") Long equipmentId,
            @Param("scheduleId") Long scheduleId,
            @Param("notificationType") NotificationType notificationType
    );

    @Query("SELECT n FROM Notification n WHERE " +
           "n.recipientStaff.id = :recipientStaffId AND " +
           "(:notificationType IS NULL OR n.notificationType = :notificationType) AND " +
           "(:priority IS NULL OR n.priority = :priority) AND " +
           "(:isRead IS NULL OR n.isRead = :isRead) AND " +
           "(:equipmentId IS NULL OR (n.equipment IS NOT NULL AND n.equipment.id = :equipmentId)) AND " +
           "(:laboratoryId IS NULL OR (n.laboratory IS NOT NULL AND n.laboratory.id = :laboratoryId)) AND " +
           "(:fromDateTime IS NULL OR n.createdAt >= :fromDateTime) AND " +
           "(:toDateTime IS NULL OR n.createdAt <= :toDateTime) " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findWithFilters(
            @Param("recipientStaffId") Long recipientStaffId,
            @Param("notificationType") NotificationType notificationType,
            @Param("priority") NotificationPriority priority,
            @Param("isRead") Boolean isRead,
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipientStaff.id = :recipientStaffId AND n.isRead = false")
    int markAllAsReadForRecipient(@Param("recipientStaffId") Long recipientStaffId, @Param("readAt") LocalDateTime readAt);
}

