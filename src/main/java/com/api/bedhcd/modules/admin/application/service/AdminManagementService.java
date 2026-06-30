package com.api.bedhcd.modules.admin.application.service;

import com.api.bedhcd.modules.admin.api.v1.dto.AdminResponse;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminRoleGroupEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminRoleGroupJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.RoleGroupJpaRepository;
import com.api.bedhcd.modules.audit.application.service.AuditLogApplicationService;
import com.api.bedhcd.modules.identity.api.v1.dto.CreateAdminRequest;
import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
import com.api.bedhcd.shared.domain.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminJpaRepository adminJpaRepository;
    private final AdminRoleGroupJpaRepository adminRoleGroupJpaRepository;
    private final RoleGroupJpaRepository roleGroupJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogApplicationService auditLogApplicationService;

    @Transactional(readOnly = true)
    public List<AdminResponse> getAllAdmins() {
        return adminJpaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminResponse getAdmin(String adminId) {
        AdminEntity admin = adminJpaRepository.findById(adminId)
                .orElseThrow(() -> IdentityException.userNotFound(adminId));
        return mapToResponse(admin);
    }

    @Transactional
    public AdminResponse createAdmin(CreateAdminRequest request) {
        if (adminJpaRepository.findByUsername(request.getUsername()).isPresent()) {
            throw IdentityException.usernameAlreadyExists(request.getUsername());
        }

        AdminEntity admin = AdminEntity.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(Role.ADMIN)
                .isActive(true)
                .department(request.getDepartment())
                .jobTitle(request.getJobTitle())
                .build();

        admin = adminJpaRepository.save(admin);

        saveAdminRoleGroups(admin.getId(), request.getRoleGroupIds());

        String payload = "Tạo mới tài khoản Admin: " + request.getUsername() + ".\n" +
                         "Họ tên: " + request.getFullName() + ".\n" +
                         "Phòng ban: " + (request.getDepartment() != null && !request.getDepartment().isBlank() ? request.getDepartment() : "Không có") + ".\n" +
                         "Chức vụ: " + (request.getJobTitle() != null && !request.getJobTitle().isBlank() ? request.getJobTitle() : "Không có") + ".\n" +
                         "Số nhóm quyền được gán: " + (request.getRoleGroupIds() != null ? request.getRoleGroupIds().size() : 0) + ".";

        logManualActivity("CREATE_ADMIN", "MANAGE_ADMIN", admin.getId(), payload);

        return mapToResponse(admin);
    }

    @Transactional
    public AdminResponse updateAdmin(String adminId, CreateAdminRequest request) {
        AdminEntity admin = adminJpaRepository.findById(adminId)
                .orElseThrow(() -> IdentityException.userNotFound(adminId));

        StringBuilder diff = new StringBuilder("Cập nhật tài khoản Admin (" + admin.getUsername() + "):\n");
        
        if (request.getFullName() != null && !java.util.Objects.equals(admin.getFullName(), request.getFullName())) {
            diff.append("- Họ tên: '").append(admin.getFullName() != null ? admin.getFullName() : "").append("' -> '").append(request.getFullName()).append("'\n");
            admin.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !java.util.Objects.equals(admin.getEmail(), request.getEmail())) {
            diff.append("- Email: '").append(admin.getEmail() != null ? admin.getEmail() : "").append("' -> '").append(request.getEmail()).append("'\n");
            admin.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            diff.append("- Mật khẩu: Đã được thay đổi\n");
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getDepartment() != null && !java.util.Objects.equals(admin.getDepartment(), request.getDepartment())) {
            diff.append("- Phòng ban: '").append(admin.getDepartment() != null ? admin.getDepartment() : "").append("' -> '").append(request.getDepartment()).append("'\n");
            admin.setDepartment(request.getDepartment());
        }
        if (request.getJobTitle() != null && !java.util.Objects.equals(admin.getJobTitle(), request.getJobTitle())) {
            diff.append("- Chức vụ: '").append(admin.getJobTitle() != null ? admin.getJobTitle() : "").append("' -> '").append(request.getJobTitle()).append("'\n");
            admin.setJobTitle(request.getJobTitle());
        }

        // Diff role groups
        List<AdminRoleGroupEntity> oldGroups = adminRoleGroupJpaRepository.findByAdminId(adminId);
        List<String> oldGroupIds = oldGroups.stream().map(AdminRoleGroupEntity::getRoleGroupId).collect(Collectors.toList());
        List<String> newGroupIds = request.getRoleGroupIds() != null ? request.getRoleGroupIds() : new java.util.ArrayList<>();
        
        List<String> addedGroups = newGroupIds.stream().filter(id -> !oldGroupIds.contains(id)).collect(Collectors.toList());
        List<String> removedGroups = oldGroupIds.stream().filter(id -> !newGroupIds.contains(id)).collect(Collectors.toList());
        
        if (!addedGroups.isEmpty() || !removedGroups.isEmpty()) {
            diff.append("- Nhóm quyền:\n");
            if (!addedGroups.isEmpty()) {
                String addedNames = addedGroups.stream().map(id -> roleGroupJpaRepository.findById(id).map(g -> g.getName()).orElse(id)).collect(Collectors.joining(", "));
                diff.append("  + Thêm: [").append(addedNames).append("]\n");
            }
            if (!removedGroups.isEmpty()) {
                String removedNames = removedGroups.stream().map(id -> roleGroupJpaRepository.findById(id).map(g -> g.getName()).orElse(id)).collect(Collectors.joining(", "));
                diff.append("  + Bớt: [").append(removedNames).append("]\n");
            }
        }

        admin = adminJpaRepository.save(admin);
        saveAdminRoleGroups(admin.getId(), request.getRoleGroupIds());

        if (diff.toString().equals("Cập nhật tài khoản Admin (" + admin.getUsername() + "):\n")) {
            diff.append("- Không có thay đổi nào được thực hiện.");
        }

        logManualActivity("UPDATE_ADMIN", "MANAGE_ADMIN", adminId, diff.toString());

        return mapToResponse(admin);
    }

    private void saveAdminRoleGroups(String adminId, List<String> roleGroupIds) {
        adminRoleGroupJpaRepository.deleteByAdminId(adminId);
        if (roleGroupIds != null && !roleGroupIds.isEmpty()) {
            List<AdminRoleGroupEntity> entities = roleGroupIds.stream()
                    .map(rgId -> AdminRoleGroupEntity.builder()
                            .adminId(adminId)
                            .roleGroupId(rgId)
                            .build())
                    .collect(Collectors.toList());
            adminRoleGroupJpaRepository.saveAll(entities);
        }
    }

    private AdminResponse mapToResponse(AdminEntity admin) {
        AdminResponse response = new AdminResponse();
        response.setId(admin.getId());
        response.setUsername(admin.getUsername());
        response.setFullName(admin.getFullName());
        response.setEmail(admin.getEmail());
        response.setRole(admin.getRole());
        response.setActive(admin.isActive());

        response.setDepartment(admin.getDepartment());
        response.setJobTitle(admin.getJobTitle());

        List<AdminRoleGroupEntity> adminGroups = adminRoleGroupJpaRepository.findByAdminId(admin.getId());
        List<String> groupIds = new ArrayList<>();
        List<String> groupNames = new ArrayList<>();
        for (AdminRoleGroupEntity ag : adminGroups) {
            groupIds.add(ag.getRoleGroupId());
            roleGroupJpaRepository.findById(ag.getRoleGroupId())
                    .ifPresent(group -> groupNames.add(group.getName()));
        }
        response.setRoleGroupIds(groupIds);
        response.setRoleGroupNames(groupNames);

        return response;
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
