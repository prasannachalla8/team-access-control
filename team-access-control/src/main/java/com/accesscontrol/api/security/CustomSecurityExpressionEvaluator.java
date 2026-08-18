package com.accesscontrol.api.security;

import com.accesscontrol.api.service.PermissionEvaluatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("securityEvaluator")
@RequiredArgsConstructor
public class CustomSecurityExpressionEvaluator {

    private final PermissionEvaluatorService permissionEvaluatorService;

    public boolean hasOrgPermission(Authentication authentication, String organizationIdStr, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            // The principal in our JwtAuthenticationFilter is stored as the user UUID
            UUID userId = (UUID) authentication.getPrincipal();
            UUID organizationId = UUID.fromString(organizationIdStr);

            return permissionEvaluatorService.hasPermission(userId, organizationId, permissionName);
        } catch (Exception e) {
            return false;
        }
    }
}