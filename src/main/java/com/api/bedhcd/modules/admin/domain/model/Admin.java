package com.api.bedhcd.modules.admin.domain.model;

import com.api.bedhcd.modules.admin.domain.exception.AdminException;
import com.api.bedhcd.shared.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.api.bedhcd.shared.domain.UuidFactory;

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

    private String department;
    private String jobTitle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private Set<AdminPermission> permissions = new HashSet<>();

    public boolean isSuperAdmin() {
        return Role.SUPER_ADMIN.equals(role);
    }

    public static Admin createNew(String username, String encodedPassword, String fullName, String email,
            String department, String jobTitle) {
        return Admin.builder()
                .id(UuidFactory.generate())
                .username(username)
                .password(encodedPassword)
                .fullName(fullName)
                .email(email)
                .role(Role.ADMIN)
                .isActive(true)
                .department(department)
                .jobTitle(jobTitle)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateProfile(String fullName, String email, String department, String jobTitle) {
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        if (this.isSuperAdmin()) {
            throw AdminException.cannotDeactivateSuperAdmin();
        }
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }
}
