package com.accesscontrol.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.accesscontrol.api.dto.PagedResponse;
import com.accesscontrol.api.dto.SessionResponse;
import com.accesscontrol.api.model.Session;
import com.accesscontrol.api.repository.SessionRepository;
import com.accesscontrol.api.service.RedisSessionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionRepository sessionRepository;
    private final RedisSessionService redisSessionService;

    @GetMapping
    public ResponseEntity<PagedResponse<SessionResponse>> getUserSessions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = (UUID) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SessionResponse> result = sessionRepository.findByUserId(userId, pageable)
                .map(SessionResponse::from);
        return ResponseEntity.ok(PagedResponse.from(result));
    }
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> revokeSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        UUID userId = (UUID) authentication.getPrincipal();

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to revoke this session");
        }

        sessionRepository.delete(session);
        redisSessionService.invalidateSession(sessionId);
        return ResponseEntity.ok("Session revoked successfully");
    }
}