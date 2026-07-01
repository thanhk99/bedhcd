package com.api.bedhcd.modules.voting.application.port;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionVote {
    private String optionId;
    private long weight;
}
