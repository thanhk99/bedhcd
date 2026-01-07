package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionResponse {
    private String id;
    private String title;
    private String description;
    private Integer displayOrder;
    private List<VotingOptionResponse> votingOptions;
    private List<UserVoteResponse> userVotes;
}
