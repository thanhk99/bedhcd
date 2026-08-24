package com.api.bedhcd.modules.participant.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "expected_attendance", indexes = {
        @Index(name = "idx_expected_meeting_cccd", columnList = "meeting_id, cccd", unique = true),
        @Index(name = "idx_expected_meeting_id", columnList = "meeting_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedAttendanceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false, length = 64)
    private String meetingId;

    @Column(name = "cccd", nullable = false, length = 32)
    private String cccd;

    @Column(name = "expected_shares", nullable = false)
    private Long expectedShares;

    @Column(name = "proxy_cccd", length = 32)
    private String proxyCccd;

    @Column(name = "proxy_shares")
    private Long proxyShares;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
