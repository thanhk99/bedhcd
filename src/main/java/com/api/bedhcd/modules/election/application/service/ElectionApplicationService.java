package com.api.bedhcd.modules.election.application.service;

import com.api.bedhcd.modules.election.api.v1.dto.CandidateRequest;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionRequest;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResponse;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResultResponse;
import com.api.bedhcd.modules.election.domain.exception.ElectionException;
import com.api.bedhcd.modules.election.domain.model.Election;
import com.api.bedhcd.modules.election.domain.repository.ElectionRepository;
import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.voting.api.v1.dto.VoteRequest;
import com.api.bedhcd.modules.voting.domain.model.Vote;
import com.api.bedhcd.modules.voting.domain.model.VotingOption;
import com.api.bedhcd.modules.voting.domain.repository.VoteRepository;
import com.api.bedhcd.shared.domain.enums.VotingOptionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElectionApplicationService {

        private final ElectionRepository electionRepository;
        private final VoteRepository voteRepository;
        private final IdentityPort identityPort;
        private final ParticipantPort participantPort;

        @Transactional(readOnly = true)
        public List<ElectionResponse> getByMeeting(String meetingId) {
                return electionRepository.findByMeetingId(meetingId).stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public ElectionResponse createElection(String meetingId, ElectionRequest request) {
                Election election = Election.builder()
                                .id(UUID.randomUUID().toString())
                                .meetingId(meetingId)
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .numSeats(request.getNumSeats())
                                .electionType(request.getType())
                                .displayOrder(request.getDisplayOrder())
                                .createdAt(LocalDateTime.now())
                                .build();
                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public ElectionResponse addCandidate(String electionId, CandidateRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                int nextOrder = request.getDisplayOrder() != null
                                ? request.getDisplayOrder()
                                : (election.getCandidates() != null ? election.getCandidates().size() + 1 : 1);

                VotingOption candidate = VotingOption.builder()
                                .id(UUID.randomUUID().toString())
                                .name(request.getFullName())
                                .description(request.getDescription())
                                .bio(request.getDescription())
                                .displayOrder(nextOrder)
                                .type(VotingOptionType.CANDIDATE)
                                .build();

                election.getCandidates().add(candidate);
                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public void submitVote(String electionId, VoteRequest request) {
                String userId = identityPort.getCurrentUserId();
                if (userId == null)
                        throw ElectionException.unauthorized();

                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                if (!participantPort.isCheckedIn(election.getMeetingId(), userId)) {
                        throw ElectionException.invalidState("Cổ đông chưa điểm danh, không thể bỏ phiếu.");
                }

                long basePower = participantPort.getVotingPower(election.getMeetingId(), userId);
                long totalPower = election.calculateVotingPower(basePower);

                if (request.getOptionVotes() == null || request.getOptionVotes().isEmpty()) {
                        throw ElectionException.invalidVoteDistribution("Phải chọn ít nhất một ứng viên");
                }

                long totalDistributed = request.getOptionVotes().stream()
                                .mapToLong(VoteRequest.OptionVoteRequest::getVoteWeight)
                                .sum();

                if (totalDistributed != totalPower) {
                        throw ElectionException.invalidVoteDistribution(
                                        "Tổng số phiếu phân bổ (" + totalDistributed
                                                        + ") không khớp với tổng quyền biểu quyết (" + totalPower + ")");
                }

                Set<String> validCandidateIds = election.getCandidates() != null
                                ? election.getCandidates().stream().map(VotingOption::getId).collect(Collectors.toSet())
                                : Collections.emptySet();

                for (VoteRequest.OptionVoteRequest optVote : request.getOptionVotes()) {
                        if (!validCandidateIds.contains(optVote.getVotingOptionId())) {
                                throw ElectionException.invalidVoteDistribution(
                                                "Ứng viên không tồn tại trong cuộc bầu cử này: " + optVote.getVotingOptionId());
                        }
                        if (optVote.getVoteWeight() < 0) {
                                throw ElectionException.invalidVoteDistribution("Số phiếu không thể âm");
                        }
                }

                // Xoá phiếu cũ
                List<Vote> oldVotes = voteRepository.findByResolutionAndUser(electionId, userId);
                oldVotes.forEach(v -> voteRepository.delete(v.getId()));

                // Lưu phiếu mới
                for (VoteRequest.OptionVoteRequest optVote : request.getOptionVotes()) {
                        if (optVote.getVoteWeight() == 0)
                                continue;
                        Vote vote = Vote.builder()
                                        .electionId(electionId)
                                        .votingOptionId(optVote.getVotingOptionId())
                                        .userId(userId)
                                        .voteWeight(optVote.getVoteWeight())
                                        .votedAt(LocalDateTime.now())
                                        .build();
                        voteRepository.save(vote);
                }
        }

        @Transactional(readOnly = true)
        public ElectionResultResponse getResults(String electionId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                List<Vote> votes = voteRepository.findByResolution(electionId);

                List<ElectionResultResponse.CandidateResult> results;
                if (election.getCandidates() != null) {
                        results = election.getCandidates().stream()
                                        .map(cand -> {
                                                long weight = votes.stream()
                                                                .filter(v -> v.getVotingOptionId().equals(cand.getId()))
                                                                .mapToLong(Vote::getVoteWeight)
                                                                .sum();
                                                return ElectionResultResponse.CandidateResult.builder()
                                                                .candidateId(cand.getId())
                                                                .candidateName(cand.getName())
                                                                .voteCount(votes.stream()
                                                                                .filter(v -> v.getVotingOptionId()
                                                                                                .equals(cand.getId()))
                                                                                .count())
                                                                .totalWeight(weight)
                                                                .build();
                                        }).collect(Collectors.toList());
                } else {
                        results = Collections.emptyList();
                }

                return ElectionResultResponse.builder()
                                .electionId(electionId)
                                .title(election.getTitle())
                                .numSeats(election.getNumSeats())
                                .results(results)
                                .totalWeight(results.stream()
                                                .mapToLong(ElectionResultResponse.CandidateResult::getTotalWeight)
                                                .sum())
                                .totalVoters(votes.stream().map(Vote::getUserId).distinct().count())
                                .build();
        }

        private ElectionResponse toResponse(Election domain) {
                return ElectionResponse.builder()
                                .id(domain.getId())
                                .meetingId(domain.getMeetingId())
                                .title(domain.getTitle())
                                .description(domain.getDescription())
                                .numSeats(domain.getNumSeats())
                                .type(domain.getElectionType())
                                .displayOrder(domain.getDisplayOrder())
                                .candidates(domain.getCandidates() == null ? Collections.emptyList()
                                                : domain.getCandidates().stream()
                                                                .map(c -> ElectionResponse.CandidateResponse.builder()
                                                                                .id(c.getId())
                                                                                .fullName(c.getName())
                                                                                .description(c.getBio())
                                                                                .build())
                                                                .collect(Collectors.toList()))
                                .build();
        }
}
