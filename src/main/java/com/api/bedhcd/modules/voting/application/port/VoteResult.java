package com.api.bedhcd.modules.voting.application.port;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteResult {
    private String optionId;
    private long totalWeight;
    private long voteCount;
}
