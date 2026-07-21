package com.api.bedhcd.modules.election.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.ElectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectionRequest {
    private String title;
    private String description;
    private ElectionType type;
    private Integer displayOrder;
}
