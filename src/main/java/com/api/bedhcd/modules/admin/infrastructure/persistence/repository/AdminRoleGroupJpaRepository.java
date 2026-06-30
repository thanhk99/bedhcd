package com.api.bedhcd.modules.admin.infrastructure.persistence.repository;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminRoleGroupEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminRoleGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminRoleGroupJpaRepository extends JpaRepository<AdminRoleGroupEntity, AdminRoleGroupId> {
    List<AdminRoleGroupEntity> findByAdminId(String adminId);
    void deleteByAdminId(String adminId);
    boolean existsByRoleGroupId(String roleGroupId);
    List<AdminRoleGroupEntity> findByRoleGroupId(String roleGroupId);

    @Modifying
    void deleteByRoleGroupId(String roleGroupId);
}
