package com.api.bedhcd.service;

import com.api.bedhcd.dto.response.DashboardStatsResponse;
import com.api.bedhcd.entity.Role;
import com.api.bedhcd.entity.enums.MeetingStatus;
import com.api.bedhcd.entity.enums.ParticipantStatus;
import com.api.bedhcd.repository.MeetingParticipantRepository;
import com.api.bedhcd.repository.MeetingRepository;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import com.api.bedhcd.entity.MeetingParticipant;
import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.enums.DelegationStatus;
import com.api.bedhcd.repository.ProxyDelegationRepository;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final UserRepository userRepository;
        private final MeetingRepository meetingRepository;
        private final MeetingParticipantRepository participantRepository;
        private final ProxyDelegationRepository proxyDelegationRepository;

        @Transactional(readOnly = true)
        public DashboardStatsResponse getSummaryStats() {
                // User Stats
                long totalShareholders = userRepository.countByRolesContaining(Role.SHAREHOLDER);
                long totalShares = userRepository.sumTotalShares();

                // Attendance Stats (lấy từ cuộc họp đang diễn ra hoặc gần nhất)
                long attendedCount = 0;
                long attendedShares = 0;
                double participationRate = 0;

                long totalShareholderCount = 0;

                com.api.bedhcd.entity.Meeting currentMeeting = meetingRepository
                                .findFirstByStatus(MeetingStatus.ONGOING)
                                .orElse(meetingRepository.findAll().stream()
                                                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                                                .findFirst()
                                                .orElse(null));

                if (currentMeeting != null) {
                        List<MeetingParticipant> attendees = participantRepository
                                        .findByMeeting_Id(currentMeeting.getId()).stream()
                                        .filter(p -> p.getStatus() == ParticipantStatus.CHECKED_IN)
                                        .toList();

                        attendedCount = attendees.size();
                        attendedShares = participantRepository.sumTotalAttendingShares(currentMeeting.getId());

                        Set<String> uniqueShareholderIds = new HashSet<>();
                        for (MeetingParticipant attendee : attendees) {
                                // Nếu người tham dự là cổ đông có cổ phần, tính là 1 cổ đông
                                if (attendee.getUser().getSharesOwned() != null
                                                && attendee.getUser().getSharesOwned() > 0) {
                                        uniqueShareholderIds.add(attendee.getUser().getId());
                                }

                                // Tìm tất cả những người đã uỷ quyền cho người này và tính họ là cổ đông đã đến
                                List<ProxyDelegation> delegations = proxyDelegationRepository
                                                .findByMeeting_IdAndProxy_IdAndStatus(currentMeeting.getId(),
                                                                attendee.getUser().getId(), DelegationStatus.ACTIVE);

                                for (ProxyDelegation delegation : delegations) {
                                        uniqueShareholderIds.add(delegation.getDelegator().getId());
                                }
                        }
                        totalShareholderCount = uniqueShareholderIds.size();

                        if (totalShares > 0) {
                                participationRate = (double) attendedShares / totalShares * 100;
                        }
                }

                // Meeting Stats
                long totalMeetings = meetingRepository.count();
                long scheduled = meetingRepository.countByStatus(MeetingStatus.SCHEDULED);
                long ongoing = meetingRepository.countByStatus(MeetingStatus.ONGOING);
                long completed = meetingRepository.countByStatus(MeetingStatus.COMPLETED);
                long cancelled = meetingRepository.countByStatus(MeetingStatus.CANCELLED);

                DashboardStatsResponse.UserStats userStats = DashboardStatsResponse.UserStats.builder()
                                .totalShareholders(totalShareholders)
                                .totalSharesRepresented(totalShares)
                                .attendedCount(attendedCount)
                                .totalShareholderCount(totalShareholderCount)
                                .attendedShares(attendedShares)
                                .participationRate(participationRate)
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
