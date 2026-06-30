package com.api.bedhcd.modules.admin.infrastructure.persistence.repository;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.RoleGroupPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleGroupPermissionJpaRepository extends JpaRepository<RoleGroupPermissionEntity, String> {
    List<RoleGroupPermissionEntity> findByRoleGroupId(String roleGroupId);
    void deleteByRoleGroupId(String roleGroupId);
}
