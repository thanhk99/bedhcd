package com.api.bedhcd.modules.meeting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingWebSocketResponse {
    private String type;
    private Payload data;
    private long timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String meetingId;
        private List<ResolutionResult> resolutionResults;
        private List<ElectionResult> electionResults;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolutionResult {
        private String resolutionId;
        private String resolutionTitle;
        private List<VoteOptionResult> results;
        private long totalVoters;
        private long totalWeight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElectionResult {
        private String electionId;
        private String electionTitle;
        private List<VoteOptionResult> results;
        private long totalVoters;
        private long totalWeight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteOptionResult {
        private String votingOptionId;
        private String votingOptionName;
        private long voteCount;
        private long totalWeight;
        private double percentage;
    }
}