package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime meetingDate;
    private String location;
    private MeetingStatus status;
    private com.api.bedhcd.entity.enums.VotingType votingType;
    private java.time.LocalDateTime votingStartTime;
    private java.time.LocalDateTime votingEndTime;
    private Integer maxSelections;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
