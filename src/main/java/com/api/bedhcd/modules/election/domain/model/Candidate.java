package com.api.bedhcd.modules.election.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {
    private String id;
    private String name;
    private String bio;
    private String description;
    private Integer displayOrder;
}
