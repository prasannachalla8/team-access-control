package com.accesscontrol.api.dto;

import com.accesscontrol.api.model.AuditLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    String userEmail,
    String organizationName,
    String action,
    String details,
    String ipAddress,
    OffsetDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getUser() != null ? log.getUser().getEmail() : "system",
            log.getOrganization().getName(),
            log.getAction(),
            log.getDetails(),
            log.getIpAddress(),
            log.getCreatedAt()
        );
    }
}