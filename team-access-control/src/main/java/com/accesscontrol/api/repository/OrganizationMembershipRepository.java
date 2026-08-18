package com.accesscontrol.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.accesscontrol.api.model.OrganizationMembership;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

    @Query("SELECT COUNT(m) > 0 FROM OrganizationMembership m " +
           "JOIN m.role r JOIN r.permissions p " +
           "WHERE m.user.id = :userId AND m.organization.id = :orgId AND p.name = :permissionName")
    boolean existsUserPermissionInOrg(
        @Param("userId") UUID userId,
        @Param("orgId") UUID orgId,
        @Param("permissionName") String permissionName
    );
    List<OrganizationMembership> findByUserId(UUID user);
    
    List<OrganizationMembership> findByOrganizationId(UUID organizationId);
    
    Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    
    Page<OrganizationMembership> findByOrganizationId(UUID organizationId, Pageable pageable);
}