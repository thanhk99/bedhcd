package com.api.bedhcd.dto.request;

import com.api.bedhcd.entity.enums.ElectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectionRequest {
    private String title;
    private String description;
    private ElectionType electionType;
    private Integer displayOrder;
}
