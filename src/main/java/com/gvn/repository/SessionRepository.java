package com.gvn.repository;

import com.gvn.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findBySessionId(String sessionId);
    Optional<Session> findByRefreshToken(String refreshToken);
    void deleteByUserId(UUID userId);
}

