package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_delegations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDelegationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private String meetingId;

    @Column(name = "delegator_id", nullable = false)
    private String delegatorId;

    @Column(name = "proxy_id", nullable = false)
    private String proxyId;

    @Column(name = "shares_delegated")
    private Long sharesDelegated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DelegationStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
