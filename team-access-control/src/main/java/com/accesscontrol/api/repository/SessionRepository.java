package com.accesscontrol.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.accesscontrol.api.model.Session;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);
    void deleteByUserId(UUID userId);
    List<Session> findByUserId(UUID userId);
    Page<Session> findByUserId(UUID userId, Pageable pageable);
}