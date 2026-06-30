package com.api.bedhcd.modules.audit.infrastructure.persistence.repository;

import com.api.bedhcd.modules.audit.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {
    List<AuditLogEntity> findByActorIdOrderByCreatedAtDesc(String actorId);
    List<AuditLogEntity> findAllByOrderByCreatedAtDesc();
}
