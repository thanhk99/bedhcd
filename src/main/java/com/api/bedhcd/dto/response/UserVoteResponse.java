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
public class UserVoteResponse {
    private String votingOptionId;
    private String votingOptionName;
    private Long voteWeight;
    private LocalDateTime votedAt;
}
