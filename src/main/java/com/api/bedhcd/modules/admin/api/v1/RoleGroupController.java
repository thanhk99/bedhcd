package com.api.bedhcd.modules.admin.api.v1;

import com.api.bedhcd.modules.admin.api.v1.dto.AssignAdminsToRoleGroupRequest;
import com.api.bedhcd.modules.admin.api.v1.dto.RoleGroupRequest;
import com.api.bedhcd.modules.admin.api.v1.dto.RoleGroupResponse;
import com.api.bedhcd.modules.admin.application.service.RoleGroupApplicationService;
import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
import com.api.bedhcd.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/role-groups")
@RequiredArgsConstructor
@Tag(name = "Role Group Management", description = "Các API quản lý nhóm phân quyền")
public class RoleGroupController {

    private final RoleGroupApplicationService roleGroupApplicationService;

    @Operation(summary = "Lấy cấu trúc quyền (Metadata) để Frontend vẽ giao diện")
    @GetMapping("/metadata")
    public ApiResponse<List<com.api.bedhcd.modules.admin.api.v1.dto.PermissionMetadataResponse>> getPermissionMetadata() {
        return ApiResponse.success(roleGroupApplicationService.getPermissionMetadata());
    }

    @Operation(summary = "Lấy danh sách Nhóm Quyền")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_ROLE_GROUP, action = ActionCode.VIEW)
    @GetMapping
    public ApiResponse<List<RoleGroupResponse>> getRoleGroups() {
        return ApiResponse.success(roleGroupApplicationService.getAllRoleGroups());
    }

    @Operation(summary = "Lấy chi tiết một Nhóm Quyền")
    @GetMapping("/{id}")
    public ApiResponse<RoleGroupResponse> getRoleGroup(@PathVariable String id) {
        return ApiResponse.success(roleGroupApplicationService.getRoleGroup(id));
    }

    @Operation(summary = "Tạo Nhóm Quyền mới")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_ROLE_GROUP, action = ActionCode.CREATE)
    @PostMapping
    public ApiResponse<RoleGroupResponse> createRoleGroup(@RequestBody RoleGroupRequest request) {
        return ApiResponse.success(roleGroupApplicationService.createRoleGroup(request));
    }

    @Operation(summary = "Cập nhật Nhóm Quyền")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_ROLE_GROUP, action = ActionCode.UPDATE)
    @PutMapping("/{id}")
    public ApiResponse<RoleGroupResponse> updateRoleGroup(@PathVariable String id, @RequestBody RoleGroupRequest request) {
        return ApiResponse.success(roleGroupApplicationService.updateRoleGroup(id, request));
    }

    @Operation(summary = "Xóa Nhóm Quyền")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_ROLE_GROUP, action = ActionCode.DELETE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoleGroup(@PathVariable String id) {
        roleGroupApplicationService.deleteRoleGroup(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Gán nhiều Admin vào Nhóm Quyền trong 1 lượt")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_ROLE_GROUP, action = ActionCode.UPDATE)
    @PostMapping("/{id}/assign-admins")
    public ApiResponse<Void> assignAdminsToRoleGroup(
            @PathVariable String id,
            @RequestBody AssignAdminsToRoleGroupRequest request) {
        roleGroupApplicationService.assignAdminsToRoleGroup(id, request);
        return ApiResponse.success(null);
    }
}
