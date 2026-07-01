package com.api.bedhcd.modules.participant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobJpaRepository extends JpaRepository<ImportJobEntity, String> {
}
