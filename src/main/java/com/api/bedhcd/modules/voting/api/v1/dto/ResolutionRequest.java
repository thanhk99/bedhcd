package com.api.bedhcd.modules.voting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionRequest {
    private String title;
    private String description;
    private Integer displayOrder;
}
