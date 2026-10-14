package com.calibratesys.entity;

import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_actor", columnList = "actor_staff_id"),
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_staff_id")
    private Staff actorStaff;

    @Column(name = "actor_identifier", length = 150)
    private String actorIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private AuditEntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(Staff actorStaff, String actorIdentifier, AuditAction action,
                    AuditEntityType entityType, Long entityId, String description,
                    String oldValues, String newValues, String ipAddress) {
        this.actorStaff = actorStaff;
        this.actorIdentifier = actorIdentifier;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.ipAddress = ipAddress;
    }

    public AuditLog(Staff actorStaff, AuditAction action,
                    AuditEntityType entityType, Long entityId, String description,
                    String oldValues, String newValues, String ipAddress) {
        this(actorStaff, actorStaff != null ? actorStaff.getEmail() : "anonymous",
                action, entityType, entityId, description, oldValues, newValues, ipAddress);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Staff getActorStaff() {
        return actorStaff;
    }

    public void setActorStaff(Staff actorStaff) {
        this.actorStaff = actorStaff;
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
