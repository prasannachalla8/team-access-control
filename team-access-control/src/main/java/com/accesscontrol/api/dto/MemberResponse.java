package com.accesscontrol.api.dto;

import com.accesscontrol.api.model.OrganizationMembership;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(
    UUID userId,
    String email,
    String roleName,
    OffsetDateTime joinedAt
) {
    public static MemberResponse from(OrganizationMembership membership) {
        return new MemberResponse(
            membership.getUser().getId(),
            membership.getUser().getEmail(),
            membership.getRole().getName(),
            membership.getCreatedAt()
        );
    }
}