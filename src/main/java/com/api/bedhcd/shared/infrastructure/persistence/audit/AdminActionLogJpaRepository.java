package com.api.bedhcd.shared.infrastructure.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminActionLogJpaRepository extends JpaRepository<AdminActionLogEntity, String> {
}
