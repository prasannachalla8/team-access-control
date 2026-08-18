package com.accesscontrol.api.dto;

import com.accesscontrol.api.model.OrganizationMembership;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MyOrganizationResponse(
    UUID id,
    String name,
    String slug,
    String roleName,
    OffsetDateTime createdAt
) {
    public static MyOrganizationResponse from(OrganizationMembership membership) {
        return new MyOrganizationResponse(
            membership.getOrganization().getId(),
            membership.getOrganization().getName(),
            membership.getOrganization().getSlug(),
            membership.getRole().getName(),
            membership.getOrganization().getCreatedAt()
        );
    }
}