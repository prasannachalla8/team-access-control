package com.accesscontrol.api.service;

import com.accesscontrol.api.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionEvaluatorService {

    private final OrganizationMembershipRepository membershipRepository;

    public boolean hasPermission(UUID userId, UUID organizationId, String permissionName) {
        if (userId == null || organizationId == null || permissionName == null) {
            return false;
        }
        
        // Query database to check if the user's role in this organization contains the permission
        return membershipRepository.existsUserPermissionInOrg(userId, organizationId, permissionName);
    }
}