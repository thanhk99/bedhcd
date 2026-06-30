package com.api.bedhcd.modules.voting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionResponse {
    private String id;
    private String meetingId;
    private String title;
    private String description;
    private Integer displayOrder;
    private List<VotingOptionResponse> options;
}
