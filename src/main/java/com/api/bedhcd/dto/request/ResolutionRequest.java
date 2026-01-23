package com.api.bedhcd.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionRequest {
    private String title;
    private String description;
    private Integer displayOrder;
}
