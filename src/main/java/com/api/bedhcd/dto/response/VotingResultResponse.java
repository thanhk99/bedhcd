package com.api.bedhcd.dto.response;

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
    private String electionId;
    private String electionTitle;
    private List<VotingOptionResult> results;
    private long totalVoters;
    private long totalWeight;
    
    // Thống kê số lượng (1 người = 1 phiếu)
    private long totalIssued;    // Phát ra (Số người tham dự/Checked-in)
    private long totalCollected; // Thu về (Số người đã bỏ phiếu)
    private long totalValid;     // Hợp lệ
    private long totalInvalid;   // Không hợp lệ

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
