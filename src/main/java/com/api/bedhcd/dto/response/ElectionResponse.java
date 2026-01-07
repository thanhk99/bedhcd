package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.ElectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectionResponse {
    private String id;
    private String title;
    private String description;
    private ElectionType electionType;
    private Integer displayOrder;
    private List<VotingOptionResponse> votingOptions;
    private List<UserVoteResponse> userVotes;
}
