package com.accesscontrol.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.accesscontrol.api.dto.AuditLogResponse;
import com.accesscontrol.api.dto.PagedResponse;
import com.accesscontrol.api.model.AuditLog;
import com.accesscontrol.api.model.Organization;
import com.accesscontrol.api.model.User;
import com.accesscontrol.api.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(User user, Organization org, String action, String details, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .organization(org)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);
    }
    
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogsForOrganization(UUID orgId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> result = auditLogRepository.findByOrganizationId(orgId, pageable);
        Page<AuditLogResponse> mapped = result.map(AuditLogResponse::from);
        return PagedResponse.from(mapped);
    }
    
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsForOrganization(UUID orgId) {
        return auditLogRepository.findByOrganizationId(orgId).stream() // adjust to your actual repo method name
                .map(AuditLogResponse::from)
                .toList();
    }
    
   
}