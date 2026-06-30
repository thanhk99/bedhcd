package com.api.bedhcd.modules.admin.domain.model;

import com.api.bedhcd.shared.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {
    private String id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private Role role; // SUPER_ADMIN or ADMIN
    private boolean isActive;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private Set<AdminPermission> permissions = new HashSet<>();

    public boolean isSuperAdmin() {
        return Role.SUPER_ADMIN.equals(role);
    }
}
