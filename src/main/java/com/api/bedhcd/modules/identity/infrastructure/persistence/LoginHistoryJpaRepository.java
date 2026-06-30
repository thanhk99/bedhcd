package com.api.bedhcd.modules.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoginHistoryJpaRepository extends JpaRepository<LoginHistoryEntity, Long> {
    List<LoginHistoryEntity> findByUserIdOrderByLoginTimeDesc(String userId);
    Optional<LoginHistoryEntity> findBySessionToken(String sessionToken);
}
