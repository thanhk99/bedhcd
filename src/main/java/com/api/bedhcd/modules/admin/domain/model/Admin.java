package com.api.bedhcd.modules.admin.domain.model;

import com.api.bedhcd.modules.admin.domain.exception.AdminException;
import com.api.bedhcd.modules.admin.domain.repository.AdminRepository;
import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
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
    private String phoneNumber;
    private Role role; // SUPER_ADMIN or ADMIN
    private boolean isActive;

    private String department;
    private String jobTitle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    @Builder.Default
    private Set<AdminPermission> permissions = new HashSet<>();

    public boolean isSuperAdmin() {
        return Role.SUPER_ADMIN.equals(role);
    }

    public static Admin createNew(String username, String encodedPassword, String fullName, String email,
            String phoneNumber, String department, String jobTitle,
            AdminRepository repository) {

        if (repository.findByUsername(username).isPresent()) {
            throw IdentityException.usernameAlreadyExists(username);
        }
        if (email != null && !email.isBlank() && repository.findByEmail(email).isPresent()) {
            throw IdentityException.emailAlreadyExists(email);
        }

        return Admin.builder()
                .id(UuidFactory.generate())
                .username(username)
                .password(encodedPassword)
                .fullName(fullName)
                .email(email)
                .phoneNumber(phoneNumber)
                .role(Role.ADMIN)
                .isActive(true)
                .department(department)
                .jobTitle(jobTitle)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateProfile(String fullName, String email, String department, String jobTitle,
            com.api.bedhcd.modules.admin.domain.repository.AdminRepository repository) {
        if (email != null && !email.isBlank() && !email.equals(this.email)) {
            if (repository.findByEmail(email).isPresent()) {
                throw IdentityException.emailAlreadyExists(email);
            }
        }

        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePhone(String phoneNumber, AdminRepository repository) {
        this.phoneNumber = phoneNumber;
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

    public void setResetToken(String token, LocalDateTime expiry) {
        this.resetToken = token;
        this.resetTokenExpiry = expiry;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isResetTokenValid(String token) {
        if (this.resetToken == null || !this.resetToken.equals(token)) {
            return false;
        }
        if (this.resetTokenExpiry == null || this.resetTokenExpiry.isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }

    public void clearResetToken() {
        this.resetToken = null;
        this.resetTokenExpiry = null;
        this.updatedAt = LocalDateTime.now();
    }
}
