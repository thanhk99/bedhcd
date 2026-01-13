package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRealtimeStatus {
    private String meetingId;

    // Results for Resolutions (Biểu quyết)
    private List<VotingResultResponse> resolutionResults;

    // Results for Elections (Bầu cử)
    private List<VotingResultResponse> electionResults;
}
