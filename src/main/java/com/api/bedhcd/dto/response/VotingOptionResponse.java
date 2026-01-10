package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.VotingOptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingOptionResponse {
    private String id;
    private String name;
    private VotingOptionType type;
    private String position;
    private String bio;
    private String photoUrl;
    private Integer displayOrder;
}
