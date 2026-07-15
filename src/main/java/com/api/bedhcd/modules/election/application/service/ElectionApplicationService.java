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
import com.api.bedhcd.modules.election.api.v1.dto.ElectionVoteRequest;
import com.api.bedhcd.modules.voting.application.port.OptionVote;
import com.api.bedhcd.modules.voting.application.port.VoteResult;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.modules.election.domain.model.Candidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.api.bedhcd.shared.domain.UuidFactory;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElectionApplicationService {

        private final ElectionRepository electionRepository;
        private final VotingPort votingPort;
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
                boolean typeExists = electionRepository.findByMeetingId(meetingId).stream()
                        .anyMatch(e -> e.getElectionType() == request.getType());
                
                if (typeExists) {
                        throw ElectionException.electionAlreadyExists(meetingId, request.getType().name());
                }

                Election election = Election.builder()
                                .id(UuidFactory.generate())
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
        public ElectionResponse updateElection(String electionId, ElectionRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));
                election.setTitle(request.getTitle());
                election.setDescription(request.getDescription());
                election.setNumSeats(request.getNumSeats());
                election.setElectionType(request.getType());
                election.setDisplayOrder(request.getDisplayOrder());
                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public ElectionResponse addCandidate(String electionId, CandidateRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                int nextOrder = request.getDisplayOrder() != null
                                ? request.getDisplayOrder()
                                : (election.getCandidates() != null ? election.getCandidates().size() + 1 : 1);

                Candidate candidate = Candidate.builder()
                                .id(UuidFactory.generate())
                                .name(request.getFullName())
                                .description(request.getDescription())
                                .bio(request.getDescription())
                                .displayOrder(nextOrder)
                                .build();

                election.getCandidates().add(candidate);
                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public ElectionResponse updateCandidate(String electionId, String candidateId, CandidateRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                Candidate candidate = election.getCandidates().stream()
                                .filter(c -> c.getId().equals(candidateId))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));

                if (request.getFullName() != null) {
                    candidate.setName(request.getFullName());
                }
                if (request.getDescription() != null) {
                    candidate.setDescription(request.getDescription());
                    candidate.setBio(request.getDescription());
                }
                if (request.getDisplayOrder() != null) {
                    candidate.setDisplayOrder(request.getDisplayOrder());
                }

                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public void submitVote(String electionId, ElectionVoteRequest request) {
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
                                .mapToLong(ElectionVoteRequest.OptionVoteRequest::getVoteWeight)
                                .sum();

                if (totalDistributed != totalPower) {
                        throw ElectionException.invalidVoteDistribution(
                                        "Tổng số phiếu phân bổ (" + totalDistributed
                                                        + ") không khớp với tổng quyền biểu quyết (" + totalPower
                                                        + ")");
                }

                Set<String> validCandidateIds = election.getCandidates() != null
                                ? election.getCandidates().stream().map(Candidate::getId).collect(Collectors.toSet())
                                : Collections.emptySet();

                List<OptionVote> optionVotes = new java.util.ArrayList<>();
                for (ElectionVoteRequest.OptionVoteRequest optVote : request.getOptionVotes()) {
                        if (!validCandidateIds.contains(optVote.getCandidateId())) {
                                throw ElectionException.invalidVoteDistribution(
                                                "Ứng viên không tồn tại trong cuộc bầu cử này: "
                                                                + optVote.getCandidateId());
                        }
                        if (optVote.getVoteWeight() < 0) {
                                throw ElectionException.invalidVoteDistribution("Số phiếu không thể âm");
                        }
                        if (optVote.getVoteWeight() > 0) {
                                optionVotes.add(OptionVote.builder()
                                                .optionId(optVote.getCandidateId())
                                                .weight(optVote.getVoteWeight())
                                                .build());
                        }
                }

                votingPort.submitVotes(electionId, userId, optionVotes);
        }

        @Transactional(readOnly = true)
        public ElectionResultResponse getResults(String electionId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                List<VoteResult> portResults = votingPort.getVotesByTarget(electionId);

                List<ElectionResultResponse.CandidateResult> results;
                if (election.getCandidates() != null) {
                        results = election.getCandidates().stream()
                                        .map(cand -> {
                                                VoteResult pr = portResults.stream()
                                                                .filter(r -> r.getOptionId().equals(cand.getId()))
                                                                .findFirst().orElse(null);
                                                long weight = pr != null ? pr.getTotalWeight() : 0;
                                                long count = pr != null ? pr.getVoteCount() : 0;
                                                return ElectionResultResponse.CandidateResult.builder()
                                                                .candidateId(cand.getId())
                                                                .candidateName(cand.getName())
                                                                .voteCount(count)
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
                                .totalVoters(votingPort.countVotersByTarget(electionId))
                                .build();
        }

        @Transactional
        public void deleteElection(String meetingId, String electionId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));
                if (!election.getMeetingId().equals(meetingId)) {
                        throw ElectionException.electionNotFound(electionId);
                }

                election.validateCanBeDeleted(votingPort.countVotersByTarget(electionId));

                electionRepository.delete(election);
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
