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

    @Transactional(readOnly = true)
    public ReportStatsResponse getVotingReportStats(String meetingId) {
        long issuedShares = participantRepository.sumTotalAttendingShares(meetingId);

        // 1. Resolution Stats (Biểu quyết)
        // Lấy danh sách ID những người đã biểu quyết ít nhất 1 tờ trình
        List<Resolution> resolutions = resolutionRepository.findByMeetingIdOrderByDisplayOrderAsc(meetingId);
        Set<String> resolutionVoterIds = new HashSet<>();
        for (Resolution res : resolutions) {
            voteRepository.findByResolution_Id(res.getId()).stream()
                    .filter(v -> v.getVoteWeight() > 0)
                    .forEach(v -> resolutionVoterIds.add(v.getUser().getId()));
        }

        // Hợp lệ = Tổng cổ phần tham dự của những người đã biểu quyết
        long validResolutionShares = participantRepository.findByMeetingId(meetingId).stream()
                .filter(p -> resolutionVoterIds.contains(p.getUser().getId()))
                .mapToLong(p -> p.getAttendingShares() != null ? p.getAttendingShares() : 0L)
                .sum();

        ReportStatsResponse.VoteStats resolutionStats = ReportStatsResponse.VoteStats.builder()
                .issuedShares(issuedShares)
                .validShares(validResolutionShares)
                .invalidShares(issuedShares - validResolutionShares)
                .collectedShares(issuedShares)
                .build();

        // 2. Election Stats (Bầu cử)
        // Công thức: Hợp lệ = Tổng(voteWeight) / Số ứng viên (theo từng đợt bầu cử)
        // Vì report tổng, ta sẽ lấy giá trị hợp lệ lớn nhất trong các đợt bầu cử làm
        // đại diện (hoặc trung bình)
        // Ở đây ưu tiên lấy đợt bầu cử có lượng phiếu tham gia nhiều nhất.
        List<Election> elections = electionRepository.findByMeetingId(meetingId);
        long maxValidElectionShares = 0;

        for (Election election : elections) {
            int candidateCount = election.getVotingOptions() != null ? election.getVotingOptions().size() : 0;
            if (candidateCount == 0)
                continue;

            long totalVoteWeightForElection = voteRepository.findByElection_Id(election.getId()).stream()
                    .mapToLong(Vote::getVoteWeight)
                    .sum();

            long validSharesForThisElection = totalVoteWeightForElection / candidateCount;
            if (validSharesForThisElection > maxValidElectionShares) {
                maxValidElectionShares = validSharesForThisElection;
            }
        }

        ReportStatsResponse.VoteStats electionStats = ReportStatsResponse.VoteStats.builder()
                .issuedShares(issuedShares)
                .validShares(maxValidElectionShares)
                .invalidShares(issuedShares - maxValidElectionShares)
                .collectedShares(issuedShares)
                .build();

        return ReportStatsResponse.builder()
                .resolutionStats(resolutionStats)
                .electionStats(electionStats)
                .build();
    }
}
