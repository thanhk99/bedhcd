package com.api.bedhcd.modules.identity.api.v1;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.identity.application.service.IdentityApplicationService;
import com.api.bedhcd.modules.identity.api.v1.dto.CreateAdminRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.UpdateUserRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.UserResponse;
import com.api.bedhcd.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/users")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityApplicationService identityService;
    private final IdentityPort identityPort;

    @GetMapping
    public ApiResponse<com.api.bedhcd.shared.dto.PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(identityService.getUsers(page, size, keyword));
    }

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile() {
        String currentUserId = identityPort.getCurrentUserId();
        return ApiResponse.success(identityService.getUserProfile(currentUserId));
    }

    @GetMapping("/search")
    public ApiResponse<java.util.List<UserResponse>> search(@RequestParam String keyword) {
        return ApiResponse.success(identityService.searchUsers(keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable String id) {
        return ApiResponse.success(identityService.getUserProfile(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> updateRoles(
            @PathVariable String id, 
            @RequestBody java.util.Set<com.api.bedhcd.shared.domain.enums.Role> roles) {
        return ApiResponse.success(identityService.updateRoles(id, roles));
    }

    /**
     * API tạo tài khoản Sub-Admin.
     * Chỉ SUPER_ADMIN mới có quyền gọi API này.
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> createSubAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ApiResponse.success(identityService.createSubAdmin(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable String id, 
            @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(identityService.updateUser(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable String id, 
            @RequestParam boolean enabled) {
        return ApiResponse.success(identityService.updateStatus(id, enabled));
    }
}
