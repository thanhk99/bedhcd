package com.api.bedhcd.modules.admin.application.service;

import com.api.bedhcd.modules.admin.api.v1.dto.AdminResponse;
import com.api.bedhcd.modules.admin.domain.exception.AdminException;
import com.api.bedhcd.modules.admin.domain.model.Admin;
import com.api.bedhcd.modules.admin.domain.repository.AdminRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminRoleGroupEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminRoleGroupJpaRepository;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.RoleGroupJpaRepository;
import com.api.bedhcd.modules.audit.application.service.AuditLogApplicationService;
import com.api.bedhcd.modules.identity.api.v1.dto.CreateAdminRequest;
import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminRepository adminRepository;
    private final AdminRoleGroupJpaRepository adminRoleGroupJpaRepository;
    private final RoleGroupJpaRepository roleGroupJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogApplicationService auditLogApplicationService;

    @Transactional(readOnly = true)
    public List<AdminResponse> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminResponse getAdmin(String adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> IdentityException.userNotFound(adminId));
        return mapToResponse(admin);
    }

    @Transactional
    public AdminResponse createAdmin(CreateAdminRequest request) {
        if (adminRepository.findByUsername(request.getUsername()).isPresent()) {
            throw IdentityException.usernameAlreadyExists(request.getUsername());
        }

        Admin admin = Admin.createNew(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getEmail(),
                request.getDepartment(),
                request.getJobTitle()
        );

        admin = adminRepository.save(admin);

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
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> IdentityException.userNotFound(adminId));

        StringBuilder diff = new StringBuilder("Cập nhật tài khoản Admin (" + admin.getUsername() + "):\n");
        
        if (request.getFullName() != null && !java.util.Objects.equals(admin.getFullName(), request.getFullName())) {
            diff.append("- Họ tên: '").append(admin.getFullName() != null ? admin.getFullName() : "").append("' -> '").append(request.getFullName()).append("'\n");
        }
        if (request.getEmail() != null && !java.util.Objects.equals(admin.getEmail(), request.getEmail())) {
            diff.append("- Email: '").append(admin.getEmail() != null ? admin.getEmail() : "").append("' -> '").append(request.getEmail()).append("'\n");
        }
        if (request.getDepartment() != null && !java.util.Objects.equals(admin.getDepartment(), request.getDepartment())) {
            diff.append("- Phòng ban: '").append(admin.getDepartment() != null ? admin.getDepartment() : "").append("' -> '").append(request.getDepartment()).append("'\n");
        }
        if (request.getJobTitle() != null && !java.util.Objects.equals(admin.getJobTitle(), request.getJobTitle())) {
            diff.append("- Chức vụ: '").append(admin.getJobTitle() != null ? admin.getJobTitle() : "").append("' -> '").append(request.getJobTitle()).append("'\n");
        }
        
        admin.updateProfile(
            request.getFullName() != null ? request.getFullName() : admin.getFullName(),
            request.getEmail() != null ? request.getEmail() : admin.getEmail(),
            request.getDepartment() != null ? request.getDepartment() : admin.getDepartment(),
            request.getJobTitle() != null ? request.getJobTitle() : admin.getJobTitle()
        );

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            diff.append("- Mật khẩu: Đã được thay đổi\n");
            admin.updatePassword(passwordEncoder.encode(request.getPassword()));
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

        admin = adminRepository.save(admin);
        saveAdminRoleGroups(admin.getId(), request.getRoleGroupIds());

        if (diff.toString().equals("Cập nhật tài khoản Admin (" + admin.getUsername() + "):\n")) {
            diff.append("- Không có thay đổi nào được thực hiện.");
        }

        logManualActivity("UPDATE_ADMIN", "MANAGE_ADMIN", adminId, diff.toString());

        return mapToResponse(admin);
    }

    @Transactional
    public void deactivateAdmin(String adminId) {
        // Kiểm tra người thực hiện phải là SUPER_ADMIN
        String actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Admin actor = adminRepository.findByUsername(actorUsername)
                .orElseThrow(() -> IdentityException.userNotFound(actorUsername));
        if (!actor.isSuperAdmin()) {
            throw AdminException.onlySuperAdminCanDeactivate();
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> IdentityException.userNotFound(adminId));
        // deactivate() trong Domain sẽ tự chặn nếu target là SUPER_ADMIN
        admin.deactivate();
        adminRepository.save(admin);
        logManualActivity("DEACTIVATE_ADMIN", "MANAGE_ADMIN", adminId, "Vô hiệu hoá tài khoản Admin: " + admin.getUsername());
    }

    @Transactional
    public void activateAdmin(String adminId) {
        // Kiểm tra người thực hiện phải là SUPER_ADMIN
        String actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Admin actor = adminRepository.findByUsername(actorUsername)
                .orElseThrow(() -> IdentityException.userNotFound(actorUsername));
        if (!actor.isSuperAdmin()) {
            throw AdminException.onlySuperAdminCanDeactivate();
        }

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> IdentityException.userNotFound(adminId));
        admin.activate();
        adminRepository.save(admin);
        logManualActivity("ACTIVATE_ADMIN", "MANAGE_ADMIN", adminId, "Kích hoạt tài khoản Admin: " + admin.getUsername());
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

    private AdminResponse mapToResponse(Admin admin) {
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
