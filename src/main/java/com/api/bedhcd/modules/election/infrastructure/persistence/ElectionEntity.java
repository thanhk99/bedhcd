package com.api.bedhcd.modules.election.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.ElectionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "elections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectionEntity {
    @Id
    private String id;

    @Column(name = "meeting_id", nullable = false)
    private String meetingId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ElectionType electionType;

    private Integer numSeats;
    private Integer displayOrder;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "election_id")
    private List<CandidateEntity> candidates;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
