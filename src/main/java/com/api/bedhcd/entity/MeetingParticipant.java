package com.api.bedhcd.entity;

import com.api.bedhcd.entity.enums.ParticipantStatus;
import com.api.bedhcd.entity.enums.ParticipationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "meeting_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "meeting_id", "user_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationType participationType;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.PENDING;

    @Column(name = "shares_owned")
    @Builder.Default
    private Long sharesOwned = 0L;

    @Column(name = "total_shares")
    @Builder.Default
    private Long totalShares = 0L;

    @Column(name = "received_proxy_shares")
    @Builder.Default
    private Long receivedProxyShares = 0L;

    @Column(name = "delegated_shares")
    @Builder.Default
    private Long delegatedShares = 0L;

    @org.hibernate.annotations.CreationTimestamp
    @Column(updatable = false)
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @org.hibernate.annotations.UpdateTimestamp
    private java.time.LocalDateTime updatedAt;

    @org.springframework.data.annotation.LastModifiedBy
    private String updatedBy;
}
