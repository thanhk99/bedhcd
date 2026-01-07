package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.CandidateRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.response.CandidateResponse;
import com.api.bedhcd.dto.response.VotingResultResponse;
import com.api.bedhcd.entity.*;
import com.api.bedhcd.entity.enums.VotingType;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.api.bedhcd.util.RandomUtil;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VotingService {

    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final VoteDraftRepository voteDraftRepository;
    private final MeetingRepository meetingRepository;
    private final VotingItemRepository votingItemRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;
    private final UserRepository userRepository;

    @Transactional
    public VotingItem createVotingItem(Long meetingId, com.api.bedhcd.dto.request.VotingItemRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        VotingItem votingItem = VotingItem.builder()
                .id(RandomUtil.generate6DigitId(votingItemRepository::existsById))
                .meeting(meeting)
                .title(request.getTitle())
                .description(request.getDescription())
                .votingType(request.getVotingType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxSelections(request.getMaxSelections() != null ? request.getMaxSelections() : 1)
                .build();

        return votingItemRepository.save(votingItem);
    }

    @Transactional
    public CandidateResponse addCandidate(Long votingItemId, CandidateRequest request) {
        java.util.Objects.requireNonNull(votingItemId, "votingItemId must not be null");
        VotingItem votingItem = votingItemRepository.findById(votingItemId)
                .orElseThrow(() -> new ResourceNotFoundException("VotingItem not found"));

        Candidate candidate = Candidate.builder()
                .id(RandomUtil.generate6DigitId(candidateRepository::existsById))
                .votingItem(votingItem)
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
    public void castVote(Long votingItemId, VoteRequest request) {
        java.util.Objects.requireNonNull(votingItemId, "votingItemId must not be null");
        VotingItem votingItem = votingItemRepository.findById(votingItemId)
                .orElseThrow(() -> new ResourceNotFoundException("VotingItem not found"));

        validateVotingTime(votingItem);

        User currentUser = getCurrentUser();

        // Calculate voting power
        long votingPower = calculateVotingPower(currentUser.getId(), votingItem.getMeeting().getId());

        // Validation logic
        validateVoteDistribution(votingItem, request, votingPower);

        // Delete existing votes and drafts
        voteRepository.deleteAll(voteRepository.findByVotingItem_IdAndUser_Id(votingItemId, currentUser.getId()));
        voteDraftRepository.deleteByVotingItem_IdAndUser_Id(votingItemId, currentUser.getId());

        // Save new votes
        LocalDateTime now = LocalDateTime.now();
        for (VoteRequest.CandidateVoteRequest candidateVote : request.getCandidateVotes()) {
            Candidate candidate = candidateRepository.findById(candidateVote.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Candidate not found: " + candidateVote.getCandidateId()));

            Vote vote = Vote.builder()
                    .votingItem(votingItem)
                    .user(currentUser)
                    .candidate(candidate)
                    .voteWeight(candidateVote.getVoteWeight())
                    .votedAt(now)
                    .build();

            voteRepository.save(vote);
        }
    }

    @Transactional
    public void saveDraft(Long votingItemId, VoteRequest request) {
        java.util.Objects.requireNonNull(votingItemId, "votingItemId must not be null");
        VotingItem votingItem = votingItemRepository.findById(votingItemId)
                .orElseThrow(() -> new ResourceNotFoundException("VotingItem not found"));

        validateVotingTime(votingItem);
        User currentUser = getCurrentUser();

        // Xóa các draft cũ
        voteDraftRepository.deleteByVotingItem_IdAndUser_Id(votingItemId, currentUser.getId());

        // Lưu draft mới
        for (VoteRequest.CandidateVoteRequest candidateVote : request.getCandidateVotes()) {
            Candidate candidate = candidateRepository.findById(candidateVote.getCandidateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

            VoteDraft draft = VoteDraft.builder()
                    .votingItem(votingItem)
                    .user(currentUser)
                    .candidate(candidate)
                    .build();

            voteDraftRepository.save(draft);
        }
    }

    public VotingResultResponse getVotingResults(Long votingItemId) {
        java.util.Objects.requireNonNull(votingItemId, "votingItemId must not be null");
        VotingItem votingItem = votingItemRepository.findById(votingItemId)
                .orElseThrow(() -> new ResourceNotFoundException("VotingItem not found"));

        List<Candidate> candidates = candidateRepository.findByVotingItem_Id(votingItemId);
        List<VotingResultResponse.CandidateResult> results = candidates.stream()
                .map(candidate -> {
                    long count = voteRepository.countByVotingItemIdAndCandidateId(votingItemId, candidate.getId());
                    Long weight = voteRepository.sumVoteWeightByVotingItemIdAndCandidateId(votingItemId,
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
                .meetingId(votingItem.getMeeting().getId())
                .meetingTitle(votingItem.getMeeting().getTitle())
                .results(results)
                .totalWeight(totalWeight)
                .build();
    }

    private void validateVotingTime(VotingItem votingItem) {
        LocalDateTime now = LocalDateTime.now();
        if (votingItem.getStartTime() != null && now.isBefore(votingItem.getStartTime())) {
            throw new BadRequestException("Outside voting time window");
        }
        if (votingItem.getEndTime() != null && now.isAfter(votingItem.getEndTime())) {
            throw new BadRequestException("Outside voting time window");
        }
    }

    private void validateVoteDistribution(VotingItem votingItem, VoteRequest request, long votingPower) {
        if (votingItem.getVotingType() == VotingType.BOARD_OF_DIRECTORS
                || votingItem.getVotingType() == VotingType.SUPERVISORY_BOARD) {
            long totalAllowedVotes = votingPower * votingItem.getMaxSelections();
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
        List<ProxyDelegation> delegations = proxyDelegationRepository.findByMeeting_IdAndProxy_Id(meetingId, userId);
        long delegatedShares = delegations.stream()
                .filter(d -> d.getStatus() == com.api.bedhcd.entity.enums.DelegationStatus.ACTIVE)
                .mapToLong(ProxyDelegation::getSharesDelegated)
                .sum();

        return baseShares + delegatedShares;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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
