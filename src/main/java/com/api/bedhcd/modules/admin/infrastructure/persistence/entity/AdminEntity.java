package com.api.bedhcd.modules.admin.infrastructure.persistence.entity;

import com.api.bedhcd.shared.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEntity {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    private String password;
    private String fullName;

    @Column(unique = true)
    private String email;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    @Builder.Default
    private boolean isActive = true;

    private String department;
    private String jobTitle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
