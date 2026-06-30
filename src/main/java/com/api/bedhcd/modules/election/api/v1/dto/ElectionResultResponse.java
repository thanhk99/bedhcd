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
public class ElectionResultResponse {
    private String electionId;
    private String title;
    private Integer numSeats;
    private List<CandidateResult> results;
    private long totalVoters;
    private long totalWeight;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidateResult {
        private String candidateId;
        private String candidateName;
        private long voteCount;
        private long totalWeight;
        private double percentage;
    }
}
