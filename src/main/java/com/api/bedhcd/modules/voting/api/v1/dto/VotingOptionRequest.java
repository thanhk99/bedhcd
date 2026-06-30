package com.api.bedhcd.modules.voting.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.VotingOptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotingOptionRequest {
    private String label;
    private String description;
    private VotingOptionType type;
}
