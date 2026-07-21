package com.api.bedhcd.modules.election.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "candidates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateEntity {

    @Id
    private String id;

    // FK plain String — đúng rule: KHÔNG dùng @ManyToOne
    @Column(name = "election_id", nullable = false)
    private String electionId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer displayOrder;
}
