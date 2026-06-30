package com.api.bedhcd.modules.identity.api.v1;

import com.api.bedhcd.modules.identity.api.v1.dto.AuthResponse;
import com.api.bedhcd.modules.identity.api.v1.dto.LoginRequest;
import com.api.bedhcd.modules.identity.api.v1.dto.RefreshTokenRequest;
import com.api.bedhcd.modules.identity.application.service.IdentityApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IdentityApplicationService identityService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(identityService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(identityService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        identityService.logout(request.getRefreshToken());
        return ApiResponse.success(null);
    }
}
