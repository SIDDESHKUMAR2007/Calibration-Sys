package com.calibratesys.service.impl;

import com.calibratesys.dto.AuditLogResponseDto;
import com.calibratesys.entity.AuditLog;
import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.AuditAction;
import com.calibratesys.entity.enums.AuditEntityType;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.AuditLogRepository;
import com.calibratesys.repository.StaffRepository;
import com.calibratesys.security.CustomUserDetails;
import com.calibratesys.service.AuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwordhash", "token", "jwt", "secret", "credentials",
            "authorization", "auth", "bearer", "apikey", "privatekey", "filecontent", "bytes"
    );

    private final AuditLogRepository auditLogRepository;
    private final StaffRepository staffRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AuditServiceImpl(AuditLogRepository auditLogRepository,
                            StaffRepository staffRepository,
                            ObjectMapper objectMapper,
                            Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.staffRepository = staffRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    private LocalDateTime getNow() {
        return LocalDateTime.now(clock);
    }

    private String getClientIp() {
        try {
            RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs instanceof ServletRequestAttributes servletAttribs) {
                HttpServletRequest req = servletAttribs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                    ip = req.getRemoteAddr();
                } else if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Staff resolveAuthenticatedStaff() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String username;
                if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                    username = userDetails.getUsername();
                } else {
                    username = auth.getName();
                }
                return staffRepository.findByEmail(username).orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String serializeAndSanitize(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof String str) {
            return str;
        }

        try {
            JsonNode tree = objectMapper.valueToTree(obj);
            sanitizeJsonNode(tree);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            log.warn("Failed to serialize audit payload: {}", e.getMessage());
            return String.valueOf(obj);
        }
    }

    private void sanitizeJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_FIELDS.contains(field.getKey().toLowerCase())) {
                    fields.remove();
                } else {
                    sanitizeJsonNode(field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                sanitizeJsonNode(child);
            }
        }
    }

    @Override
    @Transactional
    public void logEvent(AuditAction action, AuditEntityType entityType, Long entityId,
                         String description, Object oldValuesObj, Object newValuesObj) {
        try {
            Staff actor = resolveAuthenticatedStaff();
            String actorIdentifier = null;
            if (actor == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && !"anonymousUser".equals(auth.getPrincipal())) {
                    actorIdentifier = auth.getName();
                } else {
                    actorIdentifier = "SYSTEM";
                }
            } else {
                actorIdentifier = actor.getEmail();
            }

            String oldValues = serializeAndSanitize(oldValuesObj);
            String newValues = serializeAndSanitize(newValuesObj);
            String ipAddress = getClientIp();

            AuditLog auditLog = new AuditLog(
                    actor,
                    actorIdentifier,
                    action,
                    entityType,
                    entityId,
                    description,
                    oldValues,
                    newValues,
                    ipAddress
            );
            auditLog.setCreatedAt(getNow());

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to record audit log for action {}: {}", action, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthEvent(AuditAction action, Staff staff, String description, String identifier) {
        try {
            String ipAddress = getClientIp();
            String actorIdentifier = staff != null ? staff.getEmail() : (identifier != null ? identifier : "ANONYMOUS");

            AuditLog auditLog = new AuditLog(
                    staff,
                    actorIdentifier,
                    action,
                    AuditEntityType.AUTHENTICATION,
                    staff != null ? staff.getId() : null,
                    description,
                    null,
                    null,
                    ipAddress
            );
            auditLog.setCreatedAt(getNow());

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to record auth audit log for action {}: {}", action, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(AuditAction action, AuditEntityType entityType,
                                                 Long actorStaffId, LocalDate fromDate, LocalDate toDate,
                                                 int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        return auditLogRepository.findWithFilters(action, entityType, actorStaffId, fromDateTime, toDateTime, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponseDto getAuditLogById(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new ApiException("Audit log not found with ID: " + id, HttpStatus.NOT_FOUND));
        return mapToDto(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getEntityAuditLogs(AuditEntityType entityType, Long entityId, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(entityType, entityId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getActorAuditLogs(Long staffId, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        return auditLogRepository.findByActorStaffIdOrderByCreatedAtDescIdDesc(staffId, pageable)
                .map(this::mapToDto);
    }

    private AuditLogResponseDto mapToDto(AuditLog a) {
        Staff actor = a.getActorStaff();
        return new AuditLogResponseDto(
                a.getId(),
                actor != null ? actor.getId() : null,
                actor != null ? actor.getFullName() : (a.getActorIdentifier() != null ? a.getActorIdentifier() : "System"),
                actor != null ? actor.getEmail() : null,
                a.getActorIdentifier(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getDescription(),
                a.getOldValues(),
                a.getNewValues(),
                a.getIpAddress(),
                a.getCreatedAt()
        );
    }
}
