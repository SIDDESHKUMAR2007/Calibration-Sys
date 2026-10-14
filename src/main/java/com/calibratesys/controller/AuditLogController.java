package com.calibratesys.controller;

import com.calibratesys.dto.AuditLogResponseDto;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import com.calibratesys.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditService auditService;

    @Autowired
    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Retrieve paginated audit logs with optional filters.
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDto>> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) Long actorStaffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        com.calibratesys.util.PaginationValidator.validate(page, size);
        Page<AuditLogResponseDto> logs = auditService.getAuditLogs(action, entityType, actorStaffId, from, to, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * Retrieve single audit log details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogResponseDto> getAuditLogById(@PathVariable Long id) {
        AuditLogResponseDto log = auditService.getAuditLogById(id);
        return ResponseEntity.ok(log);
    }

    /**
     * Retrieve paginated audit history for a specific entity.
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLogResponseDto>> getEntityAuditLogs(
            @PathVariable AuditEntityType entityType,
            @PathVariable Long entityId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        com.calibratesys.util.PaginationValidator.validate(page, size);
        Page<AuditLogResponseDto> logs = auditService.getEntityAuditLogs(entityType, entityId, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * Retrieve paginated audit history for actions performed by a specific staff member.
     */
    @GetMapping("/actor/{staffId}")
    public ResponseEntity<Page<AuditLogResponseDto>> getActorAuditLogs(
            @PathVariable Long staffId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        com.calibratesys.util.PaginationValidator.validate(page, size);
        Page<AuditLogResponseDto> logs = auditService.getActorAuditLogs(staffId, page, size);
        return ResponseEntity.ok(logs);
    }
}
