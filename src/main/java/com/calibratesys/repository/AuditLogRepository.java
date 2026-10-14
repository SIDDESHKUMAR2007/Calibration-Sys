package com.calibratesys.repository;

import com.calibratesys.entity.AuditLog;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);


    @Query("SELECT a FROM AuditLog a LEFT JOIN a.actorStaff s WHERE " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:actorStaffId IS NULL OR (s IS NOT NULL AND s.id = :actorStaffId)) AND " +
           "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC, a.id DESC")
    Page<AuditLog> findWithFilters(
            @Param("action") AuditAction action,
            @Param("entityType") AuditEntityType entityType,
            @Param("actorStaffId") Long actorStaffId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(
            AuditEntityType entityType, Long entityId, Pageable pageable
    );

    Page<AuditLog> findByActorStaffIdOrderByCreatedAtDescIdDesc(
            Long actorStaffId, Pageable pageable
    );
}
