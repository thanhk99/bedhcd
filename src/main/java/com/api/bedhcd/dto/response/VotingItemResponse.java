package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.VotingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingItemResponse {
    private String id;
    private String title;
    private String description;
    private VotingType votingType;
    private List<VotingOptionResponse> votingOptions;
    private List<UserVoteResponse> userVotes;
}
