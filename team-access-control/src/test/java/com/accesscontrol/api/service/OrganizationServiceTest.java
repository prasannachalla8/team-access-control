package com.accesscontrol.api.service;

import com.accesscontrol.api.dto.CreateOrganizationRequest;
import com.accesscontrol.api.dto.InviteRequest;
import com.accesscontrol.api.model.*;
import com.accesscontrol.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OrganizationMembershipRepository membershipRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private EmailService emailService;

    @InjectMocks
    private OrganizationService organizationService;

    private UUID userId;
    private User user;
    private Role ownerRole;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("owner@test.com").build();
        ownerRole = Role.builder().id(UUID.randomUUID()).name("owner").build();
    }

    // --- createOrganization ---

    @Test
    void createOrganization_succeedsAndAssignsCreatorAsOwner() {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setName("Acme");
        request.setSlug("acme");

        when(organizationRepository.existsBySlug("acme")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("owner")).thenReturn(Optional.of(ownerRole));

        Organization result = organizationService.createOrganization(userId, request);

        assertThat(result.getName()).isEqualTo("Acme");
        verify(organizationRepository).save(any(Organization.class));
        verify(membershipRepository).save(any(OrganizationMembership.class));
    }

    @Test
    void createOrganization_throwsWhenSlugAlreadyTaken() {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setSlug("acme");

        when(organizationRepository.existsBySlug("acme")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.createOrganization(userId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already taken");

        verify(organizationRepository, never()).save(any());
    }

    // --- inviteTeammate ---

    @Test
    void inviteTeammate_throwsWhenOrganizationNotFound() {
        UUID orgId = UUID.randomUUID();
        InviteRequest request = new InviteRequest();
        request.setEmail("new@test.com");
        request.setRoleName("member");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.inviteTeammate(orgId, request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Organization not found");
    }

    @Test
    void inviteTeammate_throwsWhenRoleNotFound() {
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder().id(orgId).name("Acme").build();
        InviteRequest request = new InviteRequest();
        request.setEmail("new@test.com");
        request.setRoleName("nonexistent");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(roleRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.inviteTeammate(orgId, request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }

    // --- acceptInvitation ---

    @Test
    void acceptInvitation_throwsWhenTokenInvalid() {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.acceptInvitation(userId, "bad-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid invitation token");
    }

    @Test
    void acceptInvitation_throwsWhenExpired() {
        Organization org = Organization.builder().id(UUID.randomUUID()).build();
        Invitation expiredInvitation = Invitation.builder()
                .organization(org)
                .status("PENDING")
                .expiresAt(OffsetDateTime.now().minusDays(1)) // already expired
                .build();

        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredInvitation));

        assertThatThrownBy(() -> organizationService.acceptInvitation(userId, "some-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired or already used");
    }

    @Test
    void acceptInvitation_throwsWhenAlreadyMember() {
        Organization org = Organization.builder().id(UUID.randomUUID()).build();
        Invitation invitation = Invitation.builder()
                .organization(org)
                .status("PENDING")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();

        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipRepository.existsUserPermissionInOrg(userId, org.getId(), "users.read"))
                .thenReturn(true);

        assertThatThrownBy(() -> organizationService.acceptInvitation(userId, "some-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already a member");
    }

    // --- changeMemberRole (last-owner guard) ---

    @Test
    void changeMemberRole_blocksDemotingTheLastOwner() {
        UUID orgId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        OrganizationMembership ownerMembership = OrganizationMembership.builder()
                .user(User.builder().id(targetUserId).email("only-owner@test.com").build())
                .role(ownerRole)
                .build();

        Role memberRole = Role.builder().name("member").build();

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, targetUserId))
                .thenReturn(Optional.of(ownerMembership));
        when(roleRepository.findByName("member")).thenReturn(Optional.of(memberRole));
        when(membershipRepository.findByOrganizationId(orgId))
                .thenReturn(java.util.List.of(ownerMembership)); // only one owner total

        assertThatThrownBy(() ->
                organizationService.changeMemberRole(orgId, targetUserId, "member", userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at least one owner");

        verify(membershipRepository, never()).save(any());
    }

    // --- removeMember (last-owner guard) ---

    @Test
    void removeMember_blocksRemovingTheLastOwner() {
        UUID orgId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        OrganizationMembership ownerMembership = OrganizationMembership.builder()
                .user(User.builder().id(targetUserId).email("only-owner@test.com").build())
                .role(ownerRole)
                .build();

        when(membershipRepository.findByOrganizationIdAndUserId(orgId, targetUserId))
                .thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.findByOrganizationId(orgId))
                .thenReturn(java.util.List.of(ownerMembership));

        assertThatThrownBy(() ->
                organizationService.removeMember(orgId, targetUserId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("last owner");

        verify(membershipRepository, never()).delete(any());
    }
}