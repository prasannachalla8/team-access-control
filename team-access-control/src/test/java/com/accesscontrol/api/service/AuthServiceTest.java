package com.accesscontrol.api.service;

import com.accesscontrol.api.dto.LoginRequest;
import com.accesscontrol.api.dto.SignupRequest;
import com.accesscontrol.api.dto.TokenResponse;
import com.accesscontrol.api.model.User;
import com.accesscontrol.api.repository.SessionRepository;
import com.accesscontrol.api.repository.UserRepository;
import com.accesscontrol.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private RedisSessionService redisSessionService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // @Value fields aren't injected by Mockito automatically — set manually.
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L); // 7 days, arbitrary for tests
    }

    // --- signup ---

    @Test
    void signup_savesUserWithHashedPassword() {
        SignupRequest request = new SignupRequest();
        request.setEmail("new@test.com");
        request.setPassword("plaintext123");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext123")).thenReturn("hashed-value");

        authService.signup(request);

        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("new@test.com") &&
                user.getPasswordHash().equals("hashed-value") &&
                user.getStatus().equals("ACTIVE")
        ));
    }

    @Test
    void signup_throwsWhenEmailAlreadyRegistered() {
        SignupRequest request = new SignupRequest();
        request.setEmail("existing@test.com");
        request.setPassword("whatever");

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_succeedsWithCorrectCredentials() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("owner@test.com")
                .passwordHash("hashed-real-password")
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("correct-password");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-real-password")).thenReturn(true);
        when(tokenProvider.generateAccessToken(userId, "owner@test.com")).thenReturn("fake-jwt-token");

        TokenResponse result = authService.login(request, "127.0.0.1", "test-agent");

        assertThat(result.getAccessToken()).isEqualTo("fake-jwt-token");
        assertThat(result.getRefreshToken()).isNotBlank();

        verify(sessionRepository).save(any());
        verify(redisSessionService).storeActiveSession(any(), eq(604800000L));
        verify(auditLogService).logAction(eq(user), isNull(), eq("USER_LOGIN"), any(), eq("127.0.0.1"));
    }

    @Test
    void login_throwsWhenEmailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@test.com");
        request.setPassword("whatever");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void login_throwsWhenPasswordWrong_andDoesNotLeakWhichFieldWasWrong() {
        User user = User.builder()
                .email("owner@test.com")
                .passwordHash("hashed-real-password")
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-real-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "test-agent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password"); // same message as "email not found" — good security practice, worth asserting explicitly

        verify(sessionRepository, never()).save(any());
    }
}