package com.accesscontrol.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.accesscontrol.api.model.Session;

public record SessionResponse(
    UUID id,
    String userEmail,
    String ipAddress,
    String userAgent,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
            session.getId(),
            session.getUser().getEmail(), // accessed inside the transaction, safe
            session.getIpAddress(),
            session.getUserAgent(),
            session.getExpiresAt(),
            session.getCreatedAt()
        );
    }
}