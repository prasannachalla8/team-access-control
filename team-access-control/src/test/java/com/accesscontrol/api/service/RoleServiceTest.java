package com.accesscontrol.api.service;

import com.accesscontrol.api.dto.RoleResponse;
import com.accesscontrol.api.model.Permission;
import com.accesscontrol.api.model.Role;
import com.accesscontrol.api.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void getAllRoles_mapsPermissionsToNameStrings() {
        Permission readPerm = Permission.builder().name("users.read").build();
        Permission invitePerm = Permission.builder().name("users.invite").build();

        Role adminRole = Role.builder()
                .name("admin")
                .description("Administrator")
                .permissions(Set.of(readPerm, invitePerm))
                .build();

        when(roleRepository.findAll()).thenReturn(List.of(adminRole));

        List<RoleResponse> result = roleService.getAllRoles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("admin");
        assertThat(result.get(0).permissions()).containsExactlyInAnyOrder("users.read", "users.invite");
    }

    @Test
    void getAllRoles_returnsEmptyListWhenNoRolesExist() {
        when(roleRepository.findAll()).thenReturn(List.of());

        List<RoleResponse> result = roleService.getAllRoles();

        assertThat(result).isEmpty();
    }
}