package com.api.bedhcd.modules.dashboard.application.service;

import com.api.bedhcd.modules.dashboard.api.v1.dto.DashboardSummaryResponse;
import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.resolution.application.port.ResolutionPort;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private final IdentityPort identityPort;
    private final MeetingPort meetingPort;
    private final ParticipantPort participantPort;
    private final ResolutionPort resolutionPort;
    private final VotingPort votingPort;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalMeetings = meetingPort.countMeetings();
        java.util.Map<String, Long> statusCounts = meetingPort.countMeetingsGroupedByStatus();

        long checkedInCount = participantPort.countTotalCheckedIn();
        long totalSharesRepresented = participantPort.sumTotalShares();
        long attendedShares = participantPort.sumCheckedInShares();

        long totalResolutions = resolutionPort.countResolutions();
        long totalVotes = votingPort.countVotes();
        long totalShareholders = identityPort.countUsers();

        double participationRate = totalSharesRepresented > 0
                ? (double) attendedShares * 100 / totalSharesRepresented
                : 0;

        return DashboardSummaryResponse.builder()
                .userStats(DashboardSummaryResponse.UserStats.builder()
                        .totalShareholders(totalShareholders)
                        .totalSharesRepresented(totalSharesRepresented)
                        .attendedCount(checkedInCount)
                        .totalShareholderCount(totalShareholders)
                        .attendedShares(attendedShares)
                        .participationRate(participationRate)
                        .build())
                .meetingStats(DashboardSummaryResponse.MeetingStats.builder()
                        .totalMeetings(totalMeetings)
                        .statusCounts(statusCounts)
                        .build())
                .totalResolutions(totalResolutions)
                .totalVotes(totalVotes)
                .build();
    }
}
