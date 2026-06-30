package com.api.bedhcd.modules.admin.infrastructure.security;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminRoleGroupEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.RoleGroupEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.RoleGroupPermissionEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminRoleGroupJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.RoleGroupJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.RoleGroupPermissionJpaRepository;
import com.api.bedhcd.shared.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class RoleGroupPermissionAspect {

    private final AdminJpaRepository adminJpaRepository;
    private final AdminRoleGroupJpaRepository adminRoleGroupJpaRepository;
    private final RoleGroupJpaRepository roleGroupJpaRepository;
    private final RoleGroupPermissionJpaRepository roleGroupPermissionJpaRepository;

    @Before("@annotation(requireAdminPermission)")
    public void checkPermission(JoinPoint joinPoint, RequireAdminPermission requireAdminPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Unauthorized");
        }

        String username = auth.getName();
        AdminEntity admin = adminJpaRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User is not an admin"));

        if (admin.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        List<AdminRoleGroupEntity> adminGroups = adminRoleGroupJpaRepository.findByAdminId(admin.getId());
        if (adminGroups.isEmpty()) {
            throw new AccessDeniedException("Admin chưa được phân vào nhóm quyền nào");
        }

        Set<String> validGroupIds = adminGroups.stream()
                .map(AdminRoleGroupEntity::getRoleGroupId)
                .collect(Collectors.toSet());

        Set<String> activeGroupIds = new HashSet<>();
        for (String groupId : validGroupIds) {
            roleGroupJpaRepository.findById(groupId)
                    .filter(RoleGroupEntity::isActive)
                    .ifPresent(g -> activeGroupIds.add(groupId));
        }

        if (activeGroupIds.isEmpty()) {
            throw new AccessDeniedException("Các nhóm quyền của tài khoản này đã bị vô hiệu hóa (inactive)");
        }

        boolean hasPermission = false;
        for (String groupId : activeGroupIds) {
            List<RoleGroupPermissionEntity> permissions = roleGroupPermissionJpaRepository.findByRoleGroupId(groupId);
            if (permissions.stream().anyMatch(p ->
                    p.getResource() == requireAdminPermission.resource() &&
                    p.getActions().contains(requireAdminPermission.action()))) {
                hasPermission = true;
                break;
            }
        }

        if (!hasPermission) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này");
        }
    }
}
