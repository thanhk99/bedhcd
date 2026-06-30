package com.api.bedhcd.modules.meeting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
