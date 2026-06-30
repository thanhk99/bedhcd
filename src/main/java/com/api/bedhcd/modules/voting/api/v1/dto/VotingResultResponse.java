package com.api.bedhcd.modules.voting.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VotingResultResponse {
    private String meetingId;
    private String meetingTitle;
    private String resolutionId;
    private String resolutionTitle;
    private Integer displayOrder;
    private List<VotingOptionResult> results;
    private long totalVoters;
    private long totalWeight;
    
    private long totalIssued;    
    private long totalCollected; 
    private long totalValid;     
    private long totalInvalid;   

    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VotingOptionResult {
        private String votingOptionId;
        private String votingOptionName;
        private long voteCount;
        private long totalWeight;
        private double percentage;
    }
}
