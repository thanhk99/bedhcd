package com.api.bedhcd.dto;

import com.api.bedhcd.entity.Role;
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
    @Builder.Default
    private String tokenType = "Bearer";
    private String userId;
    private String email;
    private String refreshToken;
    private Set<Role> roles;

    public AuthResponse(String accessToken, String userId, String email, Set<Role> roles) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }
}
