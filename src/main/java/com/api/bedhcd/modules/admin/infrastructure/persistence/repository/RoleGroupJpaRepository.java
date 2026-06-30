package com.api.bedhcd.modules.admin.infrastructure.persistence.repository;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.RoleGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleGroupJpaRepository extends JpaRepository<RoleGroupEntity, String> {
    Optional<RoleGroupEntity> findByName(String name);
}
