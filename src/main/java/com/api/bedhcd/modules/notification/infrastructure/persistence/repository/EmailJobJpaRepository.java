package com.api.bedhcd.modules.notification.infrastructure.persistence.repository;

import com.api.bedhcd.modules.notification.infrastructure.persistence.entity.EmailJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailJobJpaRepository extends JpaRepository<EmailJobEntity, String> {
}
