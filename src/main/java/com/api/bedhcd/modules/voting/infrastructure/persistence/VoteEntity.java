package com.api.bedhcd.modules.voting.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resolution_id")
    private String resolutionId;

    @Column(name = "election_id")
    private String electionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "voting_option_id")
    private String votingOptionId;

    private Long voteWeight;
    private boolean isProxyVote;
    private String proxyFromUserId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime votedAt;
}
