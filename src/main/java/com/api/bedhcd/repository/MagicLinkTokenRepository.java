package com.api.bedhcd.repository;

import com.api.bedhcd.entity.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, Long> {
    Optional<MagicLinkToken> findByToken(String token);

    java.util.List<MagicLinkToken> findByUser_IdAndIsActiveTrueAndExpiresAtAfterOrderByExpiresAtDesc(String userId,
            java.time.LocalDateTime now);
}
