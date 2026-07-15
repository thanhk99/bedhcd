package com.api.bedhcd.modules.admin.infrastructure.persistence.repository;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.shared.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminJpaRepository extends JpaRepository<AdminEntity, String> {
    Optional<AdminEntity> findByUsername(String username);
    Optional<AdminEntity> findFirstByEmail(String email);
    Optional<AdminEntity> findByResetToken(String resetToken);
    boolean existsByRole(Role role);
}
