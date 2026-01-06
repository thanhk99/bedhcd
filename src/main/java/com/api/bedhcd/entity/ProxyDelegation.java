package com.api.bedhcd.entity;

import com.api.bedhcd.entity.enums.DelegationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_delegations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "meeting_id", "delegator_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegator_id", nullable = false)
    private User delegator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proxy_id", nullable = false)
    private User proxy;

    @Column(nullable = false)
    private Integer sharesDelegated;

    @Column(name = "authorization_document")
    private String authorizationDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DelegationStatus status = DelegationStatus.ACTIVE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime revokedAt;
}
