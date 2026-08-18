package com.accesscontrol.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisSessionService {

    private final StringRedisTemplate redisTemplate;
    private static final String SESSION_PREFIX = "session:active:";

    public void storeActiveSession(UUID sessionId, long durationInMillis) {
        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionId.toString(), "ACTIVE", Duration.ofMillis(durationInMillis));
    }

    public boolean isSessionActive(UUID sessionId) {
        String val = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId.toString());
        return "ACTIVE".equals(val);
    }

    public void invalidateSession(UUID sessionId) {
        redisTemplate.delete(SESSION_PREFIX + sessionId.toString());
    }
}