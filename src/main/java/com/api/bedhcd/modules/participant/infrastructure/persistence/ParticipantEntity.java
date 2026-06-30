package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private String meetingId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    private ParticipationType participationType;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    private Long attendingShares;
    private Long sharesOwned;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private LocalDateTime checkedInAt;
}
