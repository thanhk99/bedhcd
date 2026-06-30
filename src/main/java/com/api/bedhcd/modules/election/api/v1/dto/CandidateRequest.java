package com.api.bedhcd.modules.election.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRequest {
    private String fullName;
    private String description;
    private Integer displayOrder;
}
