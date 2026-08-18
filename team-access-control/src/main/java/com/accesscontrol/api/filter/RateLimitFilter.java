package com.accesscontrol.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    
    // Limit: Max 5 requests per 60 seconds on auth routes
    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW_SIZE = Duration.ofSeconds(60);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();

        // Apply rate limiting strictly to auth routes
        if (path.startsWith("/api/v1/auth/")) {
            String clientIp = getClientIp(request);
            String redisKey = "rate_limit:" + clientIp + ":" + path;

            Long currentCount = redisTemplate.opsForValue().increment(redisKey);
            
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(redisKey, WINDOW_SIZE);
            }

            if (currentCount != null && currentCount > MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again later.\", \"status\": 429}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}