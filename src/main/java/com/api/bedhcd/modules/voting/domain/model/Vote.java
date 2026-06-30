package com.api.bedhcd.modules.voting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vote {
    private Long id;
    private String resolutionId;
    private String electionId;
    private String userId;
    private String votingOptionId;
    private Long voteWeight;
    private boolean isProxyVote;
    private String proxyFromUserId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime votedAt;

    /**
     * Nghiệp vụ: Kiểm tra xem đây có phải là phiếu bầu cho Nghị quyết không
     */
    public boolean isResolutionVote() {
        return resolutionId != null;
    }

    /**
     * Nghiệp vụ: Kiểm tra xem đây có phải là phiếu bầu cho Bầu cử không
     */
    public boolean isElectionVote() {
        return electionId != null;
    }
}
