package com.api.bedhcd.modules.identity.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;
    public String getToken() { return accessToken; } // Alias for some clients
    @Builder.Default
    private String tokenType = "Bearer";
    private String userId;
    private String email;
    private String fullName;
    private String refreshToken;
    private Set<Role> roles;
}
