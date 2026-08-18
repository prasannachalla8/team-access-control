package com.accesscontrol.api.service;

import com.accesscontrol.api.dto.LoginRequest;
import com.accesscontrol.api.dto.SignupRequest;
import com.accesscontrol.api.dto.TokenResponse;
import com.accesscontrol.api.model.Session;
import com.accesscontrol.api.model.User;
import com.accesscontrol.api.repository.SessionRepository;
import com.accesscontrol.api.repository.UserRepository;
import com.accesscontrol.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RedisSessionService redisSessionService; 
    private final AuditLogService auditLogService;         

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .build();

        userRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String hashedRefreshToken = hashToken(rawRefreshToken);

        OffsetDateTime expiresAt = OffsetDateTime.now().plusNanos(refreshExpirationMs * 1_000_000);

        Session session = Session.builder()
                .user(user)
                .refreshTokenHash(hashedRefreshToken)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(expiresAt)
                .build();

        sessionRepository.save(session);

        // Store active session in Redis (Phase 5)
        redisSessionService.storeActiveSession(session.getId(), refreshExpirationMs);

        // Record Audit Log for login event (Phase 6)
        auditLogService.logAction(user, null, "USER_LOGIN", "User logged in successfully", ipAddress);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not hash refresh token", e);
        }
    }
}