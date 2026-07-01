package com.api.bedhcd.modules.resolution.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.VotingOptionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "voting_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotingOptionEntity {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private VotingOptionType type;
    
    private String position;
    private String bio;
    private String photoUrl;
    private Integer displayOrder;
}
