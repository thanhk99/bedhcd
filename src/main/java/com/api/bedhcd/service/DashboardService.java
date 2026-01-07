package com.api.bedhcd.service;

import com.api.bedhcd.dto.response.DashboardStatsResponse;
import com.api.bedhcd.entity.Role;
import com.api.bedhcd.entity.enums.MeetingStatus;
import com.api.bedhcd.repository.MeetingRepository;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getSummaryStats() {
        // User Stats
        long totalShareholders = userRepository.countByRolesContaining(Role.SHAREHOLDER);
        long totalShares = userRepository.sumTotalShares();

        // Meeting Stats
        long totalMeetings = meetingRepository.count();
        long scheduled = meetingRepository.countByStatus(MeetingStatus.SCHEDULED);
        long ongoing = meetingRepository.countByStatus(MeetingStatus.ONGOING);
        long completed = meetingRepository.countByStatus(MeetingStatus.COMPLETED);
        long cancelled = meetingRepository.countByStatus(MeetingStatus.CANCELLED);

        DashboardStatsResponse.UserStats userStats = DashboardStatsResponse.UserStats.builder()
                .totalShareholders(totalShareholders)
                .totalSharesRepresented(totalShares)
                .build();

        DashboardStatsResponse.MeetingStats meetingStats = DashboardStatsResponse.MeetingStats.builder()
                .totalMeetings(totalMeetings)
                .scheduled(scheduled)
                .ongoing(ongoing)
                .completed(completed)
                .cancelled(cancelled)
                .build();

        return DashboardStatsResponse.builder()
                .userStats(userStats)
                .meetingStats(meetingStats)
                .build();
    }
}
