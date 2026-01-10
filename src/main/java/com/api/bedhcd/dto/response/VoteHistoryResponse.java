package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteHistoryResponse {
    private String voteId;
    private String resolutionId;
    private String resolutionTitle;
    private String meetingId;
    private String meetingTitle;
    private String votingOptionId;
    private String votingOptionName;
    private Long voteWeight;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime votedAt;
    private com.api.bedhcd.entity.enums.VoteAction action;
}
