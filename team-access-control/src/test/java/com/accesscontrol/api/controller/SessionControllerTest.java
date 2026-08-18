package com.accesscontrol.api.controller;

import com.accesscontrol.api.model.Session;
import com.accesscontrol.api.model.User;
import com.accesscontrol.api.repository.SessionRepository;
import com.accesscontrol.api.service.RedisSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private RedisSessionService redisSessionService;
    @Mock private Authentication authentication;

    @InjectMocks
    private SessionController sessionController;

    @Test
    void revokeSession_succeedsWhenUserOwnsTheSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User owner = User.builder().id(userId).build();
        Session session = Session.builder().id(sessionId).user(owner).build();

        when(authentication.getPrincipal()).thenReturn(userId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ResponseEntity<String> response = sessionController.revokeSession(authentication, sessionId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(sessionRepository).delete(session);
        verify(redisSessionService).invalidateSession(sessionId);
    }

    @Test
    void revokeSession_throwsWhenUserDoesNotOwnTheSession() {
        UUID actualOwnerId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID(); // the attacker trying to revoke someone else's session
        UUID sessionId = UUID.randomUUID();

        User actualOwner = User.builder().id(actualOwnerId).build();
        Session session = Session.builder().id(sessionId).user(actualOwner).build();

        when(authentication.getPrincipal()).thenReturn(differentUserId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionController.revokeSession(authentication, sessionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized");

        verify(sessionRepository, never()).delete(any());
        verify(redisSessionService, never()).invalidateSession(any());
    }

    @Test
    void revokeSession_throwsWhenSessionDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(userId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionController.revokeSession(authentication, sessionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Session not found");
    }
}