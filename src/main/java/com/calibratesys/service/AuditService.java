package com.calibratesys.service;

import com.calibratesys.dto.AuditLogResponseDto;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AuditService {

    void logEvent(AuditAction action, AuditEntityType entityType, Long entityId,
                  String description, Object oldValuesObj, Object newValuesObj);

    void recordAuthEvent(AuditAction action, Staff staff, String description, String identifier);

    Page<AuditLogResponseDto> getAuditLogs(AuditAction action, AuditEntityType entityType,
                                          Long actorStaffId, LocalDate fromDate, LocalDate toDate,
                                          int page, int size);

    AuditLogResponseDto getAuditLogById(Long id);

    Page<AuditLogResponseDto> getEntityAuditLogs(AuditEntityType entityType, Long entityId,
                                                int page, int size);

    Page<AuditLogResponseDto> getActorAuditLogs(Long staffId, int page, int size);
}
