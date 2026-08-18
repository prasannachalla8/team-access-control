package com.accesscontrol.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accesscontrol.api.dto.CreateOrganizationRequest;
import com.accesscontrol.api.dto.InviteRequest;
import com.accesscontrol.api.dto.MemberResponse;
import com.accesscontrol.api.dto.MyOrganizationResponse;
import com.accesscontrol.api.dto.PagedResponse;
import com.accesscontrol.api.model.Invitation;
import com.accesscontrol.api.model.Organization;
import com.accesscontrol.api.model.OrganizationMembership;
import com.accesscontrol.api.model.Role;
import com.accesscontrol.api.model.User;
import com.accesscontrol.api.repository.InvitationRepository;
import com.accesscontrol.api.repository.OrganizationMembershipRepository;
import com.accesscontrol.api.repository.OrganizationRepository;
import com.accesscontrol.api.repository.RoleRepository;
import com.accesscontrol.api.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final InvitationRepository invitationRepository;
    private final AuditLogService auditLogService; // Added for Phase 6 Audit Logging
    private final EmailService emailService;

    @Transactional
    public Organization createOrganization(UUID userId, CreateOrganizationRequest request) {
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Organization slug is already taken");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Organization org = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .build();
        organizationRepository.save(org);

        // Fetch the 'owner' role seeded in Phase 1
        Role ownerRole = roleRepository.findByName("owner")
                .orElseThrow(() -> new RuntimeException("Default owner role not found"));

        // Assign creator as owner of the organization
        OrganizationMembership membership = OrganizationMembership.builder()
                .user(user)
                .organization(org)
                .role(ownerRole)
                .build();
        membershipRepository.save(membership);

        return org;
    }

    @Transactional
    public String inviteTeammate(UUID orgId, InviteRequest request, UUID actorUserId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new RuntimeException("Actor user not found"));

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);

        Invitation invitation = Invitation.builder()
                .organization(org)
                .email(request.getEmail())
                .role(role)
                .tokenHash(hashedToken)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .status("PENDING")
                .build();

        invitationRepository.save(invitation);

        emailService.sendInvitationEmail(request.getEmail(), org.getName(), rawToken); // replace the System.out.println line with this

        auditLogService.logAction(
                actor,
                org,
                "TEAM_INVITE",
                "Invited " + request.getEmail() + " with role " + request.getRoleName(),
                null
        );
        System.out.println("Invite link: http://localhost:5173/accept-invite?token=" + rawToken);
        return rawToken;
    }
    @Transactional
    public void acceptInvitation(UUID userId, String rawToken) {
        String hashedToken = hashToken(rawToken);
        Invitation invitation = invitationRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RuntimeException("Invalid invitation token"));

        if (!"PENDING".equals(invitation.getStatus()) || invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Invitation is expired or already used");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is already a member
        boolean alreadyMember = membershipRepository.existsUserPermissionInOrg(user.getId(), invitation.getOrganization().getId(), "users.read");
        if (alreadyMember) {
            throw new RuntimeException("User is already a member of this organization");
        }

        OrganizationMembership membership = OrganizationMembership.builder()
                .user(user)
                .organization(invitation.getOrganization())
                .role(invitation.getRole())
                .build();
        membershipRepository.save(membership);

        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);
        
        auditLogService.logAction(
                user,
                invitation.getOrganization(),
                "TEAM_JOIN",
                user.getEmail() + " accepted invitation and joined as " + invitation.getRole().getName(),
                null
        );
    }
    
    @Transactional(readOnly = true)
    public List<MyOrganizationResponse> getMyOrganizations(UUID user) {
        return membershipRepository.findByUserId(user).stream()
                .map(MyOrganizationResponse::from)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<MemberResponse> getOrganizationMembers(UUID orgId) {
        return membershipRepository.findByOrganizationId(orgId).stream()
                .map(MemberResponse::from)
                .toList();
    }
    
    @Transactional
    public void changeMemberRole(UUID orgId, UUID targetUserId, String newRoleName, UUID actorUserId) {
        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Member not found in this organization"));

        Role newRole = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + newRoleName));

        // Guard against removing the last owner — an org with zero owners is unrecoverable.
        if ("owner".equals(membership.getRole().getName()) && !"owner".equals(newRoleName)) {
            long ownerCount = membershipRepository.findByOrganizationId(orgId).stream()
                    .filter(m -> "owner".equals(m.getRole().getName()))
                    .count();
            if (ownerCount <= 1) {
                throw new RuntimeException("Cannot change role: organization must have at least one owner");
            }
        }

        String oldRoleName = membership.getRole().getName();
        membership.setRole(newRole);
        membershipRepository.save(membership);

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new RuntimeException("Actor user not found"));
        auditLogService.logAction(
                actor,
                membership.getOrganization(),
                "ROLE_CHANGE",
                membership.getUser().getEmail() + ": " + oldRoleName + " -> " + newRoleName,
                null
        );
    }

    @Transactional
    public void removeMember(UUID orgId, UUID targetUserId, UUID actorUserId) {
        OrganizationMembership membership = membershipRepository
                .findByOrganizationIdAndUserId(orgId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Member not found in this organization"));

        if ("owner".equals(membership.getRole().getName())) {
            long ownerCount = membershipRepository.findByOrganizationId(orgId).stream()
                    .filter(m -> "owner".equals(m.getRole().getName()))
                    .count();
            if (ownerCount <= 1) {
                throw new RuntimeException("Cannot remove the last owner of an organization");
            }
        }

        String removedEmail = membership.getUser().getEmail();
        Organization org = membership.getOrganization();
        membershipRepository.delete(membership);

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new RuntimeException("Actor user not found"));
        auditLogService.logAction(actor, org, "MEMBER_REMOVED", removedEmail, null);
    }
    
    @Transactional(readOnly = true)
    public PagedResponse<MemberResponse> getOrganizationMembers(UUID orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MemberResponse> result = membershipRepository.findByOrganizationId(orgId, pageable)
                .map(MemberResponse::from);
        return PagedResponse.from(result);
    }
   
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not hash token", e);
        }
    }
}