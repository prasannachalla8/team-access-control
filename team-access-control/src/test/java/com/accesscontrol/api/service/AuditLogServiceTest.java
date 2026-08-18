package com.accesscontrol.api.service;

import com.accesscontrol.api.dto.AuditLogResponse;
import com.accesscontrol.api.model.AuditLog;
import com.accesscontrol.api.model.Organization;
import com.accesscontrol.api.model.User;
import com.accesscontrol.api.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void logAction_savesAuditLogWithGivenFields() {
        User actor = User.builder().id(UUID.randomUUID()).email("owner@test.com").build();
        Organization org = Organization.builder().id(UUID.randomUUID()).name("Acme").build();

        auditLogService.logAction(actor, org, "TEAM_INVITE", "Invited someone@test.com", "127.0.0.1");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getAuditLogsForOrganization_mapsToPagedResponseCorrectly() {
        UUID orgId = UUID.randomUUID();
        User actor = User.builder().email("owner@test.com").build();
        Organization org = Organization.builder().name("Acme").build();

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .user(actor)
                .organization(org)
                .action("TEAM_INVITE")
                .details("Invited someone@test.com")
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByOrganizationId(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        var result = auditLogService.getAuditLogsForOrganization(orgId, 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).action()).isEqualTo("TEAM_INVITE");
        assertThat(result.totalElements()).isEqualTo(1);
    }
}