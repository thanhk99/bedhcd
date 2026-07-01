package com.api.bedhcd.modules.election.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectionVoteRequest {
    private List<OptionVoteRequest> optionVotes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionVoteRequest {
        private String candidateId;
        private Long voteWeight;
    }
}
