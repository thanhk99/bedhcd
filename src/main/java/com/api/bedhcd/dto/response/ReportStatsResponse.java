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
    private VoteStats boardOfDirectorsStats; // Hội đồng quản trị
    private VoteStats supervisoryBoardStats; // Ban kiểm soát

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoteStats {
        private long issuedShares; // Phát ra (Cổ phần)
        private long validShares; // Hợp lệ (Cổ phần)
        private long invalidShares; // Không hợp lệ (Cổ phần)
        private long collectedShares; // Thu về (Cổ phần)

        // Thống kê theo số lượng (1 người = 1 phiếu)
        private long issuedCount;    // Phát ra (Số người tham dự)
        private long validCount;     // Hợp lệ (Số phiếu)
        private long invalidCount;   // Không hợp lệ (Số phiếu)
        private long collectedCount; // Thu về (Số phiếu)
    }
}
