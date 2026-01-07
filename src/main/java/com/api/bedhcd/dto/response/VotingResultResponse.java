package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingResultResponse {
    private Long meetingId;
    private String meetingTitle;
    private List<CandidateResult> results;
    private long totalVoters;
    private long totalWeight;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateResult {
        private Long candidateId;
        private String candidateName;
        private long voteCount;
        private long totalWeight;
        private double percentage;
    }
}
