package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.CandidateRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.request.VotingSessionRequest;
import com.api.bedhcd.dto.response.CandidateResponse;
import com.api.bedhcd.dto.response.VotingResultResponse;
import com.api.bedhcd.dto.response.VotingSessionResponse;
import com.api.bedhcd.entity.*;
import com.api.bedhcd.entity.enums.VotingSessionStatus;
import com.api.bedhcd.entity.enums.VotingType;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VotingService {

    private final VotingSessionRepository votingSessionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final VoteDraftRepository voteDraftRepository;
    private final MeetingRepository meetingRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;
    private final UserRepository userRepository;

    @Transactional
    public VotingSessionResponse createVotingSession(VotingSessionRequest request) {
        Meeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        VotingSession session = VotingSession.builder()
                .meeting(meeting)
                .title(request.getTitle())
                .description(request.getDescription())
                .votingType(request.getVotingType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxSelections(request.getMaxSelections() != null ? request.getMaxSelections() : 1)
                .status(request.getStatus() != null ? request.getStatus() : VotingSessionStatus.PENDING)
                .build();

        session = votingSessionRepository.save(session);
        return mapToResponse(session);
    }

    @Transactional
    public CandidateResponse addCandidate(Long sessionId, CandidateRequest request) {
        VotingSession session = votingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting session not found"));

        Candidate candidate = Candidate.builder()
                .votingSession(session)
                .name(request.getName())
                .position(request.getPosition())
                .bio(request.getBio())
                .photoUrl(request.getPhotoUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        candidate = candidateRepository.save(candidate);
        return mapCandidateToResponse(candidate);
    }

    @Transactional
    public void castVote(Long sessionId, VoteRequest request) {
        VotingSession session = votingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting session not found"));

        validateVotingTime(session);

        User currentUser = getCurrentUser();

        // Calculate voting power
        long votingPower = calculateVotingPower(currentUser.getId(), session.getMeeting().getId());

        // Validation logic
        validateVoteDistribution(session, request, votingPower);

        // Delete existing votes and drafts
        voteRepository.deleteAll(voteRepository.findByVotingSessionIdAndUserId(sessionId, currentUser.getId()));
        voteDraftRepository.deleteByVotingSessionIdAndUserId(sessionId, currentUser.getId());

        // Save new votes
        LocalDateTime now = LocalDateTime.now();
        for (VoteRequest.CandidateVoteRequest candidateVote : request.getCandidateVotes()) {
            Candidate candidate = candidateRepository.findById(candidateVote.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Candidate not found: " + candidateVote.getCandidateId()));

            Vote vote = Vote.builder()
                    .votingSession(session)
                    .user(currentUser)
                    .candidate(candidate)
                    .voteWeight(candidateVote.getVoteWeight())
                    .votedAt(now)
                    .build();

            voteRepository.save(vote);
        }
    }

    @Transactional
    public void saveDraft(Long sessionId, VoteRequest request) {
        VotingSession session = votingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting session not found"));

        validateVotingTime(session);
        User currentUser = getCurrentUser();

        // Xóa các draft cũ
        voteDraftRepository.deleteByVotingSessionIdAndUserId(sessionId, currentUser.getId());

        // Lưu draft mới
        for (VoteRequest.CandidateVoteRequest candidateVote : request.getCandidateVotes()) {
            Candidate candidate = candidateRepository.findById(candidateVote.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

            VoteDraft draft = VoteDraft.builder()
                    .votingSession(session)
                    .user(currentUser)
                    .candidate(candidate)
                    .build();

            voteDraftRepository.save(draft);
        }
    }

    public VotingResultResponse getVotingResults(Long sessionId) {
        VotingSession session = votingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting session not found"));

        List<Candidate> candidates = candidateRepository.findByVotingSessionId(sessionId);
        List<VotingResultResponse.CandidateResult> results = candidates.stream()
                .map(candidate -> {
                    long count = voteRepository.countByVotingSessionIdAndCandidateId(sessionId, candidate.getId());
                    Long weight = voteRepository.sumVoteWeightByVotingSessionIdAndCandidateId(sessionId,
                            candidate.getId());
                    return VotingResultResponse.CandidateResult.builder()
                            .candidateId(candidate.getId())
                            .candidateName(candidate.getName())
                            .voteCount(count)
                            .totalWeight(weight != null ? weight : 0L)
                            .build();
                })
                .collect(Collectors.toList());

        long totalWeight = results.stream().mapToLong(VotingResultResponse.CandidateResult::getTotalWeight).sum();
        results.forEach(r -> {
            if (totalWeight > 0) {
                r.setPercentage((double) r.getTotalWeight() / totalWeight * 100);
            }
        });

        return VotingResultResponse.builder()
                .sessionId(sessionId)
                .sessionTitle(session.getTitle())
                .results(results)
                .totalWeight(totalWeight)
                .build();
    }

    private void validateVotingTime(VotingSession session) {
        if (session.getStatus() != VotingSessionStatus.ACTIVE) {
            throw new BadRequestException("Voting session is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            throw new BadRequestException("Outside voting time window");
        }
    }

    private void validateVoteDistribution(VotingSession session, VoteRequest request, long votingPower) {
        if (session.getVotingType() == VotingType.BOARD_OF_DIRECTORS
                || session.getVotingType() == VotingType.SUPERVISORY_BOARD) {
            long totalAllowedVotes = votingPower * session.getMaxSelections();
            long totalAllocatedVotes = request.getCandidateVotes().stream()
                    .mapToLong(VoteRequest.CandidateVoteRequest::getVoteWeight)
                    .sum();

            if (totalAllocatedVotes > totalAllowedVotes) {
                throw new BadRequestException("Total votes allocated (" + totalAllocatedVotes
                        + ") exceeds allowed votes (" + totalAllowedVotes + ")");
            }
        } else {
            if (request.getCandidateVotes().size() > 1) {
                throw new BadRequestException("Can only vote for one option in resolution");
            }
            // Trong Biểu quyết, voteWeight luôn bằng votingPower (toàn bộ cổ phần)
            if (!request.getCandidateVotes().isEmpty()) {
                request.getCandidateVotes().get(0).setVoteWeight((int) votingPower);
            }
        }
    }

    private long calculateVotingPower(Long userId, Long meetingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long baseShares = user.getSharesOwned() != null ? user.getSharesOwned() : 0;

        // Cộng thêm cổ phần được ủy quyền
        List<ProxyDelegation> delegations = proxyDelegationRepository.findByMeetingIdAndProxyId(meetingId, userId);
        long delegatedShares = delegations.stream()
                .filter(d -> d.getStatus() == com.api.bedhcd.entity.enums.DelegationStatus.ACTIVE)
                .mapToLong(ProxyDelegation::getSharesDelegated)
                .sum();

        return baseShares + delegatedShares;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private VotingSessionResponse mapToResponse(VotingSession session) {
        return VotingSessionResponse.builder()
                .id(session.getId())
                .meetingId(session.getMeeting().getId())
                .title(session.getTitle())
                .description(session.getDescription())
                .votingType(session.getVotingType())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .maxSelections(session.getMaxSelections())
                .status(session.getStatus())
                .candidates(
                        session.getCandidates().stream().map(this::mapCandidateToResponse).collect(Collectors.toList()))
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private CandidateResponse mapCandidateToResponse(Candidate candidate) {
        return CandidateResponse.builder()
                .id(candidate.getId())
                .name(candidate.getName())
                .position(candidate.getPosition())
                .bio(candidate.getBio())
                .photoUrl(candidate.getPhotoUrl())
                .displayOrder(candidate.getDisplayOrder())
                .build();
    }
}
