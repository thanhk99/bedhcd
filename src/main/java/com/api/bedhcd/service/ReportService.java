package com.api.bedhcd.service;

import com.api.bedhcd.dto.response.ReportStatsResponse;
import com.api.bedhcd.entity.Election;
import com.api.bedhcd.entity.Resolution;
import com.api.bedhcd.entity.Vote;
import com.api.bedhcd.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MeetingParticipantRepository participantRepository;
    private final VoteRepository voteRepository;
    private final ResolutionRepository resolutionRepository;
    private final ElectionRepository electionRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;

    @Transactional(readOnly = true)
    public ReportStatsResponse getVotingReportStats(String meetingId) {
        // Lấy toàn bộ ủy quyền active để tính toán số cổ phần nhận ủy quyền chính xác
        java.util.Map<String, Long> proxyIdToReceivedShares = proxyDelegationRepository
                .findByMeeting_IdAndStatus(meetingId, com.api.bedhcd.entity.enums.DelegationStatus.ACTIVE).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        pd -> pd.getProxy().getId(),
                        java.util.stream.Collectors.summingLong(com.api.bedhcd.entity.ProxyDelegation::getSharesDelegated)
                ));

        // Lấy toàn bộ danh sách người tham gia để tính toán map quyền biểu quyết
        List<com.api.bedhcd.entity.MeetingParticipant> allParticipants = participantRepository.findByMeetingId(meetingId);
        java.util.Map<String, Long> userIdToVotingPower = allParticipants.stream()
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getUser().getId(),
                        p -> (p.getAttendingShares() != null ? p.getAttendingShares() : 0L) +
                                proxyIdToReceivedShares.getOrDefault(p.getUser().getId(), 0L),
                        (a, b) -> a
                ));

        long issuedShares = allParticipants.stream()
                .filter(p -> p.getStatus() == com.api.bedhcd.entity.enums.ParticipantStatus.CHECKED_IN)
                .mapToLong(p -> (p.getAttendingShares() != null ? p.getAttendingShares() : 0L) +
                        proxyIdToReceivedShares.getOrDefault(p.getUser().getId(), 0L))
                .sum();

        long issuedCount = allParticipants.stream()
                .filter(p -> p.getStatus() == com.api.bedhcd.entity.enums.ParticipantStatus.CHECKED_IN)
                .count();

        // 1. Resolution Stats (Biểu quyết)
        List<Resolution> resolutions = resolutionRepository.findByMeetingIdOrderByDisplayOrderAsc(meetingId);

        Set<String> resolutionVoterIds = new HashSet<>();
        for (Resolution res : resolutions) {
            voteRepository.findByResolution_Id(res.getId()).stream()
                    .filter(v -> v.getVoteWeight() > 0)
                    .forEach(v -> resolutionVoterIds.add(v.getUser().getId()));
        }

        // Cổ phần thu về = Tổng quyền biểu quyết của những người đã biểu quyết
        long collectedResolutionShares = resolutionVoterIds.stream()
                .mapToLong(userId -> userIdToVotingPower.getOrDefault(userId, 0L))
                .sum();

        ReportStatsResponse.VoteStats resolutionStats = ReportStatsResponse.VoteStats.builder()
                .issuedShares(issuedShares)
                .validShares(collectedResolutionShares)
                .invalidShares(0L)
                .collectedShares(collectedResolutionShares)
                .issuedCount(issuedCount)
                .collectedCount(resolutionVoterIds.size())
                .validCount(resolutionVoterIds.size())
                .invalidCount(0)
                .build();

        // 2. Election Stats (Phân tách HĐQT và BKS)
        List<Election> elections = electionRepository.findByMeetingId(meetingId);

        ReportStatsResponse.VoteStats bodStats = calculateStatsForElectionType(elections, meetingId,
                com.api.bedhcd.entity.enums.ElectionType.BOARD_OF_DIRECTORS, issuedShares, issuedCount, userIdToVotingPower);
        ReportStatsResponse.VoteStats sbStats = calculateStatsForElectionType(elections, meetingId,
                com.api.bedhcd.entity.enums.ElectionType.SUPERVISORY_BOARD, issuedShares, issuedCount, userIdToVotingPower);

        return ReportStatsResponse.builder()
                .resolutionStats(resolutionStats)
                .boardOfDirectorsStats(bodStats)
                .supervisoryBoardStats(sbStats)
                .build();
    }

    private ReportStatsResponse.VoteStats calculateStatsForElectionType(List<Election> allElections, String meetingId,
            com.api.bedhcd.entity.enums.ElectionType type, long issuedShares, long issuedCount,
            java.util.Map<String, Long> userIdToShares) {
        List<Election> filteredElections = allElections.stream()
                .filter(e -> e.getElectionType() == type)
                .collect(java.util.stream.Collectors.toList());

        long maxValidShares = 0;
        long representativeCollectedCount = 0;
        long representativeValidCount = 0;
        long representativeInvalidCount = 0;

        for (Election election : filteredElections) {
            List<Vote> votes = voteRepository.findByElection_Id(election.getId());
            int candidateCount = election.getVotingOptions() != null ? election.getVotingOptions().size() : 0;
            int seats = election.getNumSeats() != null ? election.getNumSeats() : candidateCount;

            if (candidateCount == 0)
                continue;

            long totalVoteWeightForElection = votes.stream()
                    .mapToLong(com.api.bedhcd.entity.Vote::getVoteWeight)
                    .sum();

            java.util.Map<String, List<com.api.bedhcd.entity.Vote>> votesByUser = votes.stream()
                    .filter(v -> v.getVoteWeight() > 0)
                    .collect(java.util.stream.Collectors.groupingBy(v -> v.getUser().getId()));

            // Cổ phần thu về = Tổng phiếu chia cho số ghế
            long collectedShares = (seats > 0) ? (totalVoteWeightForElection / seats) : totalVoteWeightForElection;
            long collectedCount = votesByUser.size();

            if (collectedCount > representativeCollectedCount || (collectedCount == representativeCollectedCount
                    && collectedShares > maxValidShares)) {
                maxValidShares = collectedShares;
                representativeCollectedCount = collectedCount;
                representativeValidCount = collectedCount; // Hiện tại Hợp lệ = Thu về
                representativeInvalidCount = 0;
            }
        }

        return ReportStatsResponse.VoteStats.builder()
                .issuedShares(issuedShares)
                .validShares(maxValidShares)
                .invalidShares(0L)
                .collectedShares(maxValidShares)
                .issuedCount(issuedCount)
                .collectedCount(representativeCollectedCount)
                .validCount(representativeValidCount)
                .invalidCount(0)
                .build();
    }
}
