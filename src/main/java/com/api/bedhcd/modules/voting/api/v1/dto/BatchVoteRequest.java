package com.api.bedhcd.modules.voting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchVoteRequest {
    private List<ItemVote> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemVote {
        private String itemId; 
        private VoteRequest voteRequest;
    }
}
