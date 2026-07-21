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
public class MeetingRealtimeResponse {
    private String meetingId;
    private String title;
    private String status;

    private AttendanceStats attendance;
    private VotingStats voting;
    private List<ResolutionStats> resolutions;
    private List<ElectionStats> elections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceStats {
        private long totalParticipants;
        private long checkedInCount;
        private long totalShares;
        private long checkedInShares;
        private double participationRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VotingStats {
        private long totalResolutions;
        private long totalVotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolutionStats {
        private String resolutionId;
        private String title;
        private String description;
        private Integer displayOrder;
        private List<OptionInfo> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionInfo {
        private String optionId;
        private String name;
        private String type;
        private Integer displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElectionStats {
        private String electionId;
        private String title;
        private String electionType;
        private List<CandidateInfo> candidates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateInfo {
        private String candidateId;
        private String name;
        private String description;
        private Integer displayOrder;
    }
}
