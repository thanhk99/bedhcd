package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportStatsResponse {
    private VoteStats resolutionStats;
    private VoteStats electionStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoteStats {
        private long issuedShares; // Phát ra
        private long validShares; // Hợp lệ
        private long invalidShares; // Không hợp lệ (Không biểu quyết/bầu cử)
        private long collectedShares; // Thu về (Hợp lệ + Không hợp lệ)
    }
}
