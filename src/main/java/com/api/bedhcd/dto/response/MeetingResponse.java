package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.MeetingStatus;
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
public class MeetingResponse {
    private String id;
    private String meetingCode;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private MeetingStatus status;
    private List<ResolutionResponse> resolutions;
    private List<ElectionResponse> elections;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
