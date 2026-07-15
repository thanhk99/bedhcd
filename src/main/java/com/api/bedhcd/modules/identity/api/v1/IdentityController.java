package com.api.bedhcd.modules.identity.api.v1;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.identity.application.service.IdentityApplicationService;
import com.api.bedhcd.modules.identity.api.v1.dto.UserResponse;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityApplicationService identityService;
    private final IdentityPort identityPort;

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile() {
        String currentUserId = identityPort.getCurrentUserId();
        return ApiResponse.success(identityService.getUserProfile(currentUserId));
    }
}
