package com.api.bedhcd.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(unique = true, nullable = false, length = 100)
    private String phoneNumber;

    @Column(unique = true, nullable = false, length = 100)
    private String investorCode;

    @Column(unique = true, nullable = false, length = 100)
    private String cccd;

    @Column(unique = true, nullable = false, length = 100)
    private String dateOfIssue;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String fullName;

    @Column(name = "shareholder_code", unique = true, length = 50)
    private String shareholderCode;

    @Column(name = "shares_owned")
    @Builder.Default
    private Integer sharesOwned = 0;

    @Column(name = "received_proxy_shares")
    @Builder.Default
    private Integer receivedProxyShares = 0;

    @Column(name = "delegated_shares")
    @Builder.Default
    private Integer delegatedShares = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
