package com.api.bedhcd.modules.admin.api.v1;

import com.api.bedhcd.modules.admin.application.service.AdminApplicationService;
import com.api.bedhcd.modules.identity.api.v1.dto.AuthResponse;
import com.api.bedhcd.modules.identity.api.v1.dto.LoginRequest;
import com.api.bedhcd.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "Các API xác thực dành riêng cho Admin")
public class AdminAuthController {

    private final AdminApplicationService adminApplicationService;

    @Operation(summary = "Đăng nhập dành cho Admin")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(adminApplicationService.login(request, httpRequest));
    }

    @Operation(summary = "Đổi mật khẩu dành cho Admin")
    @org.springframework.web.bind.annotation.PutMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestBody com.api.bedhcd.modules.identity.api.v1.dto.ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication) {
        adminApplicationService.changePassword(authentication.getName(), request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Làm mới access token cho Admin")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody com.api.bedhcd.modules.identity.api.v1.dto.RefreshTokenRequest request) {
        return ApiResponse.success(adminApplicationService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "Đăng xuất Admin")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody com.api.bedhcd.modules.identity.api.v1.dto.RefreshTokenRequest request) {
        adminApplicationService.logout(request.getRefreshToken());
        return ApiResponse.success(null);
    }
}
