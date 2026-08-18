package com.accesscontrol.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accesscontrol.api.dto.AuditLogResponse;
import com.accesscontrol.api.dto.CreateOrganizationRequest;
import com.accesscontrol.api.dto.InviteRequest;
import com.accesscontrol.api.dto.MemberResponse;
import com.accesscontrol.api.dto.MyOrganizationResponse;
import com.accesscontrol.api.dto.PagedResponse;
import com.accesscontrol.api.model.Organization;
import com.accesscontrol.api.service.AuditLogService;
import com.accesscontrol.api.service.OrganizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<Organization> createOrganization(
            Authentication authentication,
            @Valid @RequestBody CreateOrganizationRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        Organization org = organizationService.createOrganization(userId, request);
        return ResponseEntity.ok(org);
    }
    
    @GetMapping
    public ResponseEntity<List<MyOrganizationResponse>> getMyOrganizations(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(organizationService.getMyOrganizations(userId));
    }

    @PostMapping("/{orgId}/invite")
    @PreAuthorize("@securityEvaluator.hasOrgPermission(authentication, #orgId, 'users.invite')")
    public ResponseEntity<String> inviteTeammate(
            Authentication authentication,
            @PathVariable UUID orgId,
            @Valid @RequestBody InviteRequest request) {
        UUID actorUserId = (UUID) authentication.getPrincipal();
        String rawToken = organizationService.inviteTeammate(orgId, request, actorUserId);
        return ResponseEntity.ok("Invitation token generated: " + rawToken);
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<String> acceptInvitation(
            Authentication authentication,
            @RequestParam String token) {
        UUID userId = (UUID) authentication.getPrincipal();
        organizationService.acceptInvitation(userId, token);
        return ResponseEntity.ok("Successfully joined organization");
    }

    @GetMapping("/{orgId}/audit-logs")
    @PreAuthorize("@securityEvaluator.hasOrgPermission(authentication, #orgId, 'audit.view')")
    public ResponseEntity<PagedResponse<AuditLogResponse>> getAuditLogs(
            Authentication authentication,
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditLogService.getAuditLogsForOrganization(orgId, page, size));
    }
    
    @GetMapping("/{orgId}/members")
    @PreAuthorize("@securityEvaluator.hasOrgPermission(authentication, #orgId, 'users.read')")
    public ResponseEntity<PagedResponse<MemberResponse>> getMembers(
            Authentication authentication,
            @PathVariable UUID orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(organizationService.getOrganizationMembers(orgId, page, size));
    }
    
    @PutMapping("/{orgId}/members/{userId}/role")
    @PreAuthorize("@securityEvaluator.hasOrgPermission(authentication, #orgId, 'roles.assign')")
    public ResponseEntity<String> changeMemberRole(
            Authentication authentication,
            @PathVariable UUID orgId,
            @PathVariable UUID userId,
            @RequestParam String roleName) {
        UUID actorUserId = (UUID) authentication.getPrincipal();
        organizationService.changeMemberRole(orgId, userId, roleName, actorUserId);
        return ResponseEntity.ok("Role updated");
    }

    @DeleteMapping("/{orgId}/members/{userId}")
    @PreAuthorize("@securityEvaluator.hasOrgPermission(authentication, #orgId, 'users.remove')")
    public ResponseEntity<String> removeMember(
            Authentication authentication,
            @PathVariable UUID orgId,
            @PathVariable UUID userId) {
        UUID actorUserId = (UUID) authentication.getPrincipal();
        organizationService.removeMember(orgId, userId, actorUserId);
        return ResponseEntity.ok("Member removed");
    }
}

