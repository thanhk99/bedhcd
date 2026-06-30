package com.api.bedhcd.modules.dashboard.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private UserStats userStats;
    private MeetingStats meetingStats;
    private long totalResolutions;
    private long totalVotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private long totalShareholders;
        private long totalSharesRepresented;
        private long attendedCount;
        private long totalShareholderCount; // Có thể trùng với totalShareholders
        private long attendedShares;
        private double participationRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingStats {
        private long totalMeetings;
        private long scheduled;
        private long ongoing;
        private long completed;
        private long cancelled;
    }
}
