package com.api.bedhcd.modules.identity.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    private String password;
    
    @Column(nullable = false)
    private String fullName;
    
    private String email;
    
    private String phoneNumber;
    
    private String address;
    
    @Column(unique = true)
    private String cccd;
    
    private String investorCode;
    
    private Long sharesOwned;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    @Builder.Default
    private boolean enabled = true;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
