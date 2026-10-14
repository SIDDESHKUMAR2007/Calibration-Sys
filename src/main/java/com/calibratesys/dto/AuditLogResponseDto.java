package com.calibratesys.dto;

import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;

import java.time.LocalDateTime;

public class AuditLogResponseDto {

    private Long id;
    private Long actorStaffId;
    private String actorStaffName;
    private String actorStaffEmail;
    private String actorIdentifier;
    private AuditAction action;
    private AuditEntityType entityType;
    private Long entityId;
    private String description;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLogResponseDto() {
    }

    public AuditLogResponseDto(Long id, Long actorStaffId, String actorStaffName, String actorStaffEmail,
                               String actorIdentifier, AuditAction action, AuditEntityType entityType,
                               Long entityId, String description, String oldValues, String newValues,
                               String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.actorStaffId = actorStaffId;
        this.actorStaffName = actorStaffName;
        this.actorStaffEmail = actorStaffEmail;
        this.actorIdentifier = actorIdentifier;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorStaffId() {
        return actorStaffId;
    }

    public void setActorStaffId(Long actorStaffId) {
        this.actorStaffId = actorStaffId;
    }

    public String getActorStaffName() {
        return actorStaffName;
    }

    public void setActorStaffName(String actorStaffName) {
        this.actorStaffName = actorStaffName;
    }

    public String getActorStaffEmail() {
        return actorStaffEmail;
    }

    public void setActorStaffEmail(String actorStaffEmail) {
        this.actorStaffEmail = actorStaffEmail;
    }

    public String getActorIdentifier() {
        return actorIdentifier;
    }

    public void setActorIdentifier(String actorIdentifier) {
        this.actorIdentifier = actorIdentifier;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(AuditEntityType entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
