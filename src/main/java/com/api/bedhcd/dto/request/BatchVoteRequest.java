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
public class BatchVoteRequest {
    private List<ItemVote> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemVote {
        private String itemId; // ID của Resolution hoặc Election
        private VoteRequest voteRequest; // Dữ liệu bình chọn (optionVotes)
    }
}
