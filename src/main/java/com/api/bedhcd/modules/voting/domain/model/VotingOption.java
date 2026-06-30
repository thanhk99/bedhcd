package com.api.bedhcd.modules.voting.domain.model;

import com.api.bedhcd.shared.domain.enums.VotingOptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotingOption {
    private String id;
    private String name;
    private String description;
    private VotingOptionType type;
    private String position;
    private String bio;
    private String photoUrl;
    private Integer displayOrder;
}
