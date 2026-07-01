package com.api.bedhcd.modules.resolution.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "resolutions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionEntity {
    @Id
    private String id;
    
    @Column(name = "meeting_id", nullable = false)
    private String meetingId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private Integer displayOrder;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "resolution_id")
    private List<VotingOptionEntity> options;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
