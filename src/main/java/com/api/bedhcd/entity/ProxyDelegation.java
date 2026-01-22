package com.api.bedhcd.entity;

import com.api.bedhcd.entity.enums.DelegationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_delegations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
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
    private Long sharesDelegated;

    @Column(name = "authorization_document")
    private String authorizationDocument;

    @Column(name = "authorization_date")
    private java.time.LocalDate authorizationDate;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DelegationStatus status = DelegationStatus.ACTIVE;

    @org.hibernate.annotations.CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @org.springframework.data.annotation.CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @org.springframework.data.annotation.LastModifiedBy
    private String updatedBy;

    private LocalDateTime revokedAt;
}
