package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.VotingSessionStatus;
import com.api.bedhcd.entity.enums.VotingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingSessionResponse {
    private Long id;
    private Long meetingId;
    private String title;
    private String description;
    private VotingType votingType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxSelections;
    private VotingSessionStatus status;
    private List<CandidateResponse> candidates;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
