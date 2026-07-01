package com.api.bedhcd.modules.admin.application.service;

import com.api.bedhcd.modules.admin.api.v1.dto.AdminPermissionDto;
import com.api.bedhcd.modules.admin.api.v1.dto.AssignAdminsToRoleGroupRequest;
import com.api.bedhcd.modules.admin.api.v1.dto.PermissionMetadataResponse;
import com.api.bedhcd.modules.admin.api.v1.dto.RoleGroupRequest;
import com.api.bedhcd.modules.admin.api.v1.dto.RoleGroupResponse;
import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminRoleGroupEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.RoleGroupEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.RoleGroupPermissionEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminRoleGroupJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.RoleGroupJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.RoleGroupPermissionJpaRepository;
import com.api.bedhcd.modules.audit.application.service.AuditLogApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.api.bedhcd.modules.admin.domain.exception.AdminException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleGroupApplicationService {

        private final RoleGroupJpaRepository roleGroupJpaRepository;
        private final RoleGroupPermissionJpaRepository roleGroupPermissionJpaRepository;
        private final AdminRoleGroupJpaRepository adminRoleGroupJpaRepository;
        private final AdminJpaRepository adminJpaRepository;
        private final AuditLogApplicationService auditLogApplicationService;

        @Transactional(readOnly = true)
        public List<RoleGroupResponse> getAllRoleGroups() {
                return roleGroupJpaRepository.findAll().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public RoleGroupResponse getRoleGroup(String id) {
                RoleGroupEntity entity = roleGroupJpaRepository.findById(id)
                                .orElseThrow(() -> AdminException.roleGroupNotFound(id));
                return mapToResponse(entity);
        }

        @Transactional
        public RoleGroupResponse createRoleGroup(RoleGroupRequest request) {
                if (roleGroupJpaRepository.findByName(request.getName()).isPresent()) {
                        throw AdminException.roleGroupNameExists(request.getName());
                }

                RoleGroupEntity entity = RoleGroupEntity.builder()
                                .name(request.getName())
                                .description(request.getDescription())
                                .isActive(request.isActive())
                                .build();
                entity = roleGroupJpaRepository.save(entity);

                savePermissions(entity.getId(), request.getPermissions());

                String payload = "Tạo mới nhóm quyền: " + request.getName() + ".\n" +
                                 "Mô tả: " + request.getDescription() + ".\n" +
                                 "Số lượng tài nguyên được cấp quyền: " + (request.getPermissions() != null ? request.getPermissions().size() : 0) + ".";
                logManualActivity("CREATE_ROLE_GROUP", "MANAGE_ROLE_GROUP", entity.getId(), payload);

                return mapToResponse(entity);
        }

        @Transactional
        public RoleGroupResponse updateRoleGroup(String id, RoleGroupRequest request) {
                RoleGroupEntity entity = roleGroupJpaRepository.findById(id)
                                .orElseThrow(() -> AdminException.roleGroupNotFound(id));

                if (!entity.getName().equals(request.getName())
                                && roleGroupJpaRepository.findByName(request.getName()).isPresent()) {
                        throw AdminException.roleGroupNameExists(request.getName());
                }

                List<RoleGroupPermissionEntity> oldPermissions = roleGroupPermissionJpaRepository.findByRoleGroupId(id);
                StringBuilder diff = new StringBuilder("Cập nhật nhóm quyền:\n");

                if (!entity.getName().equals(request.getName())) {
                        diff.append("- Tên: '").append(entity.getName()).append("' -> '").append(request.getName()).append("'\n");
                }
                if (!java.util.Objects.equals(entity.getDescription(), request.getDescription())) {
                        diff.append("- Mô tả: '").append(entity.getDescription() != null ? entity.getDescription() : "null").append("' -> '").append(request.getDescription() != null ? request.getDescription() : "null").append("'\n");
                }
                if (entity.isActive() != request.isActive()) {
                        diff.append("- Trạng thái Active: ").append(entity.isActive() ? "Bật" : "Tắt").append(" -> ").append(request.isActive() ? "Bật" : "Tắt").append("\n");
                }

                entity.setName(request.getName());
                entity.setDescription(request.getDescription());
                entity.setActive(request.isActive());
                entity = roleGroupJpaRepository.save(entity);

                savePermissions(id, request.getPermissions());

                // Diff permissions
                Map<ResourceCode, Set<ActionCode>> oldPermMap = oldPermissions.stream()
                        .collect(Collectors.toMap(RoleGroupPermissionEntity::getResource, RoleGroupPermissionEntity::getActions));

                Map<ResourceCode, Set<ActionCode>> newPermMap = request.getPermissions() == null ? java.util.Collections.emptyMap() :
                        request.getPermissions().stream()
                        .collect(Collectors.toMap(AdminPermissionDto::getResource, AdminPermissionDto::getActions));

                boolean permChanged = false;
                StringBuilder permDiff = new StringBuilder("- Quyền hạn:\n");

                for (Map.Entry<ResourceCode, Set<ActionCode>> entry : oldPermMap.entrySet()) {
                        ResourceCode res = entry.getKey();
                        Set<ActionCode> oldActions = entry.getValue();
                        Set<ActionCode> newActions = newPermMap.getOrDefault(res, java.util.Collections.emptySet());
                        
                        Set<ActionCode> removedActions = oldActions.stream().filter(a -> !newActions.contains(a)).collect(Collectors.toSet());
                        if (!removedActions.isEmpty()) {
                                permChanged = true;
                                permDiff.append("  + Bớt quyền [").append(res).append("]: ").append(removedActions).append("\n");
                        }
                }

                for (Map.Entry<ResourceCode, Set<ActionCode>> entry : newPermMap.entrySet()) {
                        ResourceCode res = entry.getKey();
                        Set<ActionCode> newActions = entry.getValue();
                        Set<ActionCode> oldActions = oldPermMap.getOrDefault(res, java.util.Collections.emptySet());
                        
                        Set<ActionCode> addedActions = newActions.stream().filter(a -> !oldActions.contains(a)).collect(Collectors.toSet());
                        if (!addedActions.isEmpty()) {
                                permChanged = true;
                                permDiff.append("  + Thêm quyền [").append(res).append("]: ").append(addedActions).append("\n");
                        }
                }

                if (permChanged) {
                        diff.append(permDiff);
                }

                if (diff.toString().equals("Cập nhật nhóm quyền:\n")) {
                        diff.append("- Không có thay đổi nào được thực hiện.");
                }

                logManualActivity("UPDATE_ROLE_GROUP", "MANAGE_ROLE_GROUP", id, diff.toString());

                return mapToResponse(entity);
        }

        @Transactional
        public void deleteRoleGroup(String id) {
                RoleGroupEntity entity = roleGroupJpaRepository.findById(id)
                                .orElseThrow(() -> AdminException.roleGroupNotFound(id));

                if (adminRoleGroupJpaRepository.existsByRoleGroupId(id)) {
                        throw AdminException.roleGroupAssignedToAdmins();
                }

                String payload = "Xóa nhóm quyền: " + entity.getName();

                roleGroupPermissionJpaRepository.deleteByRoleGroupId(id);
                roleGroupJpaRepository.delete(entity);

                logManualActivity("DELETE_ROLE_GROUP", "MANAGE_ROLE_GROUP", id, payload);
        }

        @Transactional
        public void assignAdminsToRoleGroup(String roleGroupId, AssignAdminsToRoleGroupRequest request) {
                // Kiểm tra nhóm quyền có tồn tại không
                RoleGroupEntity roleGroup = roleGroupJpaRepository.findById(roleGroupId)
                                .orElseThrow(() -> AdminException.roleGroupNotFound(roleGroupId));

                List<String> newAdminIds = request.getAdminIds() != null ? request.getAdminIds() : java.util.Collections.emptyList();

                // Validate các admin id có tồn tại không
                for (String adminId : newAdminIds) {
                        if (!adminJpaRepository.existsById(adminId)) {
                                throw AdminException.adminNotFound(adminId);
                        }
                }

                // Lấy danh sách admin hiện tại trong nhóm để tạo audit diff
                List<AdminRoleGroupEntity> currentAssignments = adminRoleGroupJpaRepository.findByRoleGroupId(roleGroupId);
                List<String> oldAdminIds = currentAssignments.stream()
                                .map(AdminRoleGroupEntity::getAdminId)
                                .collect(Collectors.toList());

                List<String> addedAdminIds = newAdminIds.stream()
                                .filter(id -> !oldAdminIds.contains(id))
                                .collect(Collectors.toList());
                List<String> removedAdminIds = oldAdminIds.stream()
                                .filter(id -> !newAdminIds.contains(id))
                                .collect(Collectors.toList());

                // Xóa toàn bộ assignment cũ và lưu lại danh sách mới
                adminRoleGroupJpaRepository.deleteByRoleGroupId(roleGroupId);
                if (!newAdminIds.isEmpty()) {
                        List<AdminRoleGroupEntity> entities = newAdminIds.stream()
                                        .map(adminId -> AdminRoleGroupEntity.builder()
                                                        .adminId(adminId)
                                                        .roleGroupId(roleGroupId)
                                                        .build())
                                        .collect(Collectors.toList());
                        adminRoleGroupJpaRepository.saveAll(entities);
                }

                // Audit log
                StringBuilder payload = new StringBuilder("Gán admin vào nhóm quyền: " + roleGroup.getName() + ".\n");
                if (!addedAdminIds.isEmpty()) {
                        String addedNames = addedAdminIds.stream()
                                        .map(id -> adminJpaRepository.findById(id)
                                                        .map(a -> a.getUsername()).orElse(id))
                                        .collect(Collectors.joining(", "));
                        payload.append("Thêm admin: [").append(addedNames).append("].\n");
                }
                if (!removedAdminIds.isEmpty()) {
                        String removedNames = removedAdminIds.stream()
                                        .map(id -> adminJpaRepository.findById(id)
                                                        .map(a -> a.getUsername()).orElse(id))
                                        .collect(Collectors.joining(", "));
                        payload.append("Bớt admin: [").append(removedNames).append("].\n");
                }
                if (addedAdminIds.isEmpty() && removedAdminIds.isEmpty()) {
                        payload.append("Không có thay đổi nào.");
                }

                logManualActivity("ASSIGN_ADMINS_TO_ROLE_GROUP", "MANAGE_ROLE_GROUP", roleGroupId, payload.toString());
        }

        private void savePermissions(String roleGroupId, List<AdminPermissionDto> permissions) {
                roleGroupPermissionJpaRepository.deleteByRoleGroupId(roleGroupId);
                if (permissions != null && !permissions.isEmpty()) {
                        List<RoleGroupPermissionEntity> entities = permissions.stream()
                                        .map(p -> RoleGroupPermissionEntity.builder()
                                                        .roleGroupId(roleGroupId)
                                                        .resource(p.getResource())
                                                        .actions(p.getActions())
                                                        .build())
                                        .collect(Collectors.toList());
                        roleGroupPermissionJpaRepository.saveAll(entities);
                }
        }

        private RoleGroupResponse mapToResponse(RoleGroupEntity entity) {
                RoleGroupResponse response = new RoleGroupResponse();
                response.setId(entity.getId());
                response.setName(entity.getName());
                response.setDescription(entity.getDescription());
                response.setActive(entity.isActive());
                response.setCreatedAt(entity.getCreatedAt());
                response.setUpdatedAt(entity.getUpdatedAt());

                List<AdminPermissionDto> permissions = roleGroupPermissionJpaRepository
                                .findByRoleGroupId(entity.getId()).stream()
                                .map(p -> {
                                        AdminPermissionDto dto = new AdminPermissionDto();
                                        dto.setResource(p.getResource());
                                        dto.setActions(p.getActions());
                                        return dto;
                                }).collect(Collectors.toList());

                response.setPermissions(permissions);
                return response;
        }

        @Transactional(readOnly = true)
        public List<PermissionMetadataResponse> getPermissionMetadata() {
                return java.util.Arrays.stream(ResourceCode.values())
                                .map(resource -> new PermissionMetadataResponse(resource, resource.getAllowedActions()))
                                .collect(Collectors.toList());
        }

        private void logManualActivity(String action, String resource, String targetId, String payload) {
                try {
                        String actorUsername = "SYSTEM";
                        if (SecurityContextHolder.getContext().getAuthentication() != null) {
                                actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
                        }
                        
                        String ipAddress = null;
                        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                        if (attributes != null) {
                                HttpServletRequest request = attributes.getRequest();
                                ipAddress = request.getHeader("X-Forwarded-For");
                                if (ipAddress == null || ipAddress.isEmpty()) {
                                        ipAddress = request.getRemoteAddr();
                                }
                        }
                        
                        auditLogApplicationService.logActionAsync(actorUsername, action, resource, targetId, payload, ipAddress);
                } catch (Exception e) {
                        System.err.println("Failed to log manual activity: " + e.getMessage());
                }
        }
}

