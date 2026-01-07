package com.api.bedhcd.dto.request;

import com.api.bedhcd.entity.enums.VotingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingItemRequest {
    private String title;
    private String description;
    private VotingType votingType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxSelections;
}
