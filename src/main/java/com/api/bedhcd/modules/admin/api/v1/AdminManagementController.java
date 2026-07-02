package com.api.bedhcd.modules.admin.api.v1;

import com.api.bedhcd.modules.admin.api.v1.dto.AdminPermissionDto;
import com.api.bedhcd.modules.admin.api.v1.dto.AdminResponse;
import com.api.bedhcd.modules.admin.application.service.AdminManagementService;
import com.api.bedhcd.modules.identity.api.v1.dto.CreateAdminRequest;
import com.api.bedhcd.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/management")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Các API dành cho SUPER_ADMIN để quản lý tài khoản và quyền của ADMIN")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @Operation(summary = "Lấy danh sách Admin")
    @GetMapping
    public ApiResponse<List<AdminResponse>> getAdmins() {
        return ApiResponse.success(adminManagementService.getAllAdmins());
    }

    @Operation(summary = "Lấy thông tin chi tiết một Admin")
    @GetMapping("/{adminId}")
    public ApiResponse<AdminResponse> getAdmin(@PathVariable String adminId) {
        return ApiResponse.success(adminManagementService.getAdmin(adminId));
    }

    @Operation(summary = "Tạo tài khoản Admin mới")
    @PostMapping
    public ApiResponse<AdminResponse> createAdmin(@RequestBody CreateAdminRequest request) {
        return ApiResponse.success(adminManagementService.createAdmin(request));
    }

    @Operation(summary = "Cập nhật tài khoản Admin")
    @PutMapping("/{adminId}")
    public ApiResponse<AdminResponse> updateAdmin(@PathVariable String adminId, @RequestBody CreateAdminRequest request) {
        return ApiResponse.success(adminManagementService.updateAdmin(adminId, request));
    }

    @Operation(summary = "Vô hiệu hoá tài khoản Admin")
    @PutMapping("/{adminId}/deactivate")
    public ApiResponse<Void> deactivateAdmin(@PathVariable String adminId) {
        adminManagementService.deactivateAdmin(adminId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Kích hoạt tài khoản Admin")
    @PutMapping("/{adminId}/activate")
    public ApiResponse<Void> activateAdmin(@PathVariable String adminId) {
        adminManagementService.activateAdmin(adminId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Xoá tài khoản Admin")
    @DeleteMapping("/{adminId}")
    public ApiResponse<Void> deleteAdmin(@PathVariable String adminId) {
        adminManagementService.deleteAdmin(adminId);
        return ApiResponse.success(null);
    }

}
