package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private UserStats userStats;
    private MeetingStats meetingStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        private long totalShareholders;
        private long totalSharesRepresented;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MeetingStats {
        private long totalMeetings;
        private long scheduled;
        private long ongoing;
        private long completed;
        private long cancelled;
    }
}
