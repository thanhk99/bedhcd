package com.api.bedhcd.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRequest {
    private List<OptionVoteRequest> optionVotes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionVoteRequest {
        private String votingOptionId;
        private Long voteWeight;
    }
}
