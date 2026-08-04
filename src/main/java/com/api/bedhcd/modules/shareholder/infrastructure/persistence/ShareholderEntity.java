package com.api.bedhcd.modules.shareholder.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.Role;
import com.api.bedhcd.shared.domain.enums.ShareholderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareholderEntity {
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Column(name = "split_account", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean splitAccount = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "shareholder_status", nullable = false, length = 20)
    @org.hibernate.annotations.ColumnDefault("'ACTIVE'")
    @Builder.Default
    private ShareholderStatus shareholderStatus = ShareholderStatus.ACTIVE;

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
