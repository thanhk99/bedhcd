package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.VotingOptionRequest;
import com.api.bedhcd.dto.request.ElectionRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.response.VotingOptionResponse;
import com.api.bedhcd.dto.response.ElectionResponse;
import com.api.bedhcd.dto.response.UserVoteResponse;
import com.api.bedhcd.dto.response.VotingResultResponse;
import com.api.bedhcd.entity.*;
import com.api.bedhcd.entity.MeetingParticipant;
import com.api.bedhcd.entity.enums.VoteAction;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.*;
import com.api.bedhcd.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ElectionService {

        private final ElectionRepository electionRepository;
        private final VotingOptionRepository votingOptionRepository;
        private final VoteRepository voteRepository;
        private final VoteDraftRepository voteDraftRepository;
        private final MeetingRepository meetingRepository;
        private final UserRepository userRepository;
        private final VoteLogRepository voteLogRepository;
        private final MeetingParticipantRepository meetingParticipantRepository;

        @Transactional
        public ElectionResponse createElection(String meetingId, ElectionRequest request) {
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                Election election = Election.builder()
                                .id(RandomUtil.generate6DigitId(electionRepository::existsById))
                                .meeting(meeting)
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .electionType(request.getElectionType())
                                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                                .build();

                election = electionRepository.save(election);
                return mapElectionToResponse(election);
        }

        public ElectionResponse getElectionById(String electionId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));
                return mapElectionToResponse(election);
        }

        @Transactional
        public VotingOptionResponse addVotingOption(String electionId, VotingOptionRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

                VotingOption option = VotingOption.builder()
                                .id(RandomUtil.generate6DigitId(votingOptionRepository::existsById))
                                .election(election)
                                .name(request.getName())
                                .position(request.getPosition())
                                .bio(request.getBio())
                                .photoUrl(request.getPhotoUrl())
                                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                                .build();

                option = votingOptionRepository.save(option);
                return mapVotingOptionToResponse(option);
        }

        @Transactional
        public void castVote(String electionId, VoteRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

                User currentUser = getCurrentUser();
                int multiplier = election.getVotingOptions() != null ? election.getVotingOptions().size() : 0;
                long votingPower = calculateVotingPower(currentUser.getId(), election.getMeeting().getId(),
                                multiplier);

                // Validation
                validateVoteDistribution(election, request, votingPower);

                // Get existing votes
                List<Vote> existingVotes = voteRepository.findByElection_IdAndUser_Id(electionId, currentUser.getId());
                Map<String, Vote> existingVoteMap = existingVotes.stream()
                                .collect(Collectors.toMap(v -> v.getVotingOption().getId(), v -> v));

                // Delete drafts
                voteDraftRepository.deleteByElection_IdAndUser_Id(electionId, currentUser.getId());

                // Process new votes
                LocalDateTime now = LocalDateTime.now();
                List<VoteRequest.OptionVoteRequest> optionVotes = request.getOptionVotes();
                int selectionCount = optionVotes.size();
                Set<String> newOptionIds = new HashSet<>();

                for (VoteRequest.OptionVoteRequest optionVote : optionVotes) {
                        String optionId = optionVote.getVotingOptionId();
                        newOptionIds.add(optionId);

                        VotingOption option = votingOptionRepository.findById(optionId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Voting option not found: " + optionId));

                        long assignedWeight = optionVote.getVoteWeight() != null
                                        ? optionVote.getVoteWeight().longValue()
                                        : (selectionCount > 0 ? votingPower / selectionCount : 0);

                        Vote vote;
                        VoteAction action;

                        // Check if vote exists for this option
                        if (existingVoteMap.containsKey(optionId)) {
                                // UPDATE existing vote
                                vote = existingVoteMap.get(optionId);
                                vote.setVoteWeight(assignedWeight);
                                vote.setVotedAt(now);
                                action = VoteAction.VOTE_CHANGED;
                        } else {
                                // INSERT new vote
                                vote = Vote.builder()
                                                .election(election)
                                                .user(currentUser)
                                                .votingOption(option)
                                                .voteWeight(assignedWeight)
                                                .votedAt(now)
                                                .build();
                                action = VoteAction.VOTE_CAST;
                        }

                        vote = voteRepository.save(vote);

                        // Log the action
                        VoteLog log = VoteLog.builder()
                                        .user(currentUser)
                                        .election(election)
                                        .vote(vote)
                                        .action(action)
                                        .votingOption(option)
                                        .build();
                        voteLogRepository.save(log);
                }

                // Set weight = 0 for options no longer selected
                for (Vote oldVote : existingVotes) {
                        String oldOptionId = oldVote.getVotingOption().getId();
                        if (!newOptionIds.contains(oldOptionId) && oldVote.getVoteWeight() > 0) {
                                oldVote.setVoteWeight(0L);
                                oldVote.setVotedAt(now);
                                voteRepository.save(oldVote);

                                // Log VOTE_CHANGED with weight 0
                                VoteLog zeroLog = VoteLog.builder()
                                                .user(currentUser)
                                                .election(election)
                                                .vote(oldVote)
                                                .action(VoteAction.VOTE_CHANGED)
                                                .votingOption(oldVote.getVotingOption())
                                                .build();
                                voteLogRepository.save(zeroLog);
                        }
                }
        }

        @Transactional
        public void saveDraft(String electionId, VoteRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

                User currentUser = getCurrentUser();
                voteDraftRepository.deleteByElection_IdAndUser_Id(electionId, currentUser.getId());

                for (VoteRequest.OptionVoteRequest optionVote : request.getOptionVotes()) {
                        VotingOption option = votingOptionRepository.findById(optionVote.getVotingOptionId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Voting option not found"));

                        VoteDraft draft = VoteDraft.builder()
                                        .election(election)
                                        .user(currentUser)
                                        .votingOption(option)
                                        .build();

                        voteDraftRepository.save(draft);
                }
        }

        public VotingResultResponse getVotingResults(String electionId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

                List<Vote> votes = voteRepository.findByElection_Id(electionId);
                List<VotingOption> options = election.getVotingOptions();

                long totalVoters = votes.stream()
                                .map(v -> v.getUser().getId())
                                .distinct()
                                .count();

                List<VotingResultResponse.VotingOptionResult> results = options.stream()
                                .map(option -> {
                                        long totalOptionWeight = votes.stream()
                                                        .filter(v -> v.getVotingOption() != null && v.getVotingOption()
                                                                        .getId().equals(option.getId()))
                                                        .mapToLong(Vote::getVoteWeight)
                                                        .sum();

                                        long votersForOption = votes.stream()
                                                        .filter(v -> v.getVotingOption() != null && v.getVotingOption()
                                                                        .getId().equals(option.getId()))
                                                        .map(v -> v.getUser().getId())
                                                        .distinct()
                                                        .count();

                                        return VotingResultResponse.VotingOptionResult.builder()
                                                        .votingOptionId(option.getId())
                                                        .votingOptionName(option.getName())
                                                        .voteCount(votersForOption)
                                                        .totalWeight(totalOptionWeight)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                long totalWeight = results.stream().mapToLong(VotingResultResponse.VotingOptionResult::getTotalWeight)
                                .sum();
                results.forEach(r -> {
                        if (totalWeight > 0) {
                                r.setPercentage((double) r.getTotalWeight() / totalWeight * 100);
                        }
                });

                return VotingResultResponse.builder()
                                .meetingId(election.getMeeting().getId())
                                .meetingTitle(election.getMeeting().getTitle())
                                .electionId(election.getId())
                                .electionTitle(election.getTitle())
                                .results(results)
                                .totalVoters(totalVoters)
                                .totalWeight(totalWeight)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        private void validateVoteDistribution(Election election, VoteRequest request, long votingPower) {
                if (request.getOptionVotes() == null || request.getOptionVotes().isEmpty()) {
                        return;
                }

                long totalAllocatedVotes = request.getOptionVotes().stream()
                                .mapToLong(ov -> ov.getVoteWeight() != null ? ov.getVoteWeight() : 0)
                                .sum();

                if (totalAllocatedVotes > votingPower) {
                        throw new BadRequestException("Total votes allocated (" + totalAllocatedVotes
                                        + ") exceeds allowed votes (" + votingPower + ")");
                }
        }

        private long calculateVotingPower(String userId, String meetingId, Integer multiplier) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                MeetingParticipant participant = getOrCreateParticipant(meeting, user);

                long baseShares = participant.getSharesOwned() != null ? participant.getSharesOwned() : 0;
                long receivedShares = participant.getReceivedProxyShares() != null
                                ? participant.getReceivedProxyShares()
                                : 0;

                long totalShares = baseShares + receivedShares;
                return totalShares * (multiplier != null ? multiplier : 1);
        }

        private MeetingParticipant getOrCreateParticipant(Meeting meeting, User user) {
                return meetingParticipantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), user.getId())
                                .orElseGet(() -> {
                                        MeetingParticipant participant = MeetingParticipant.builder()
                                                        .meeting(meeting)
                                                        .user(user)
                                                        .sharesOwned(user.getSharesOwned() != null
                                                                        ? user.getSharesOwned()
                                                                        : 0L)
                                                        .receivedProxyShares(0L)
                                                        .delegatedShares(0L)
                                                        .participationType(
                                                                        com.api.bedhcd.entity.enums.ParticipationType.DIRECT)
                                                        .status(com.api.bedhcd.entity.enums.ParticipantStatus.PENDING)
                                                        .build();
                                        return meetingParticipantRepository.save(participant);
                                });
        }

        private User getCurrentUser() {
                return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }

        private VotingOptionResponse mapVotingOptionToResponse(VotingOption option) {
                return VotingOptionResponse.builder()
                                .id(option.getId())
                                .name(option.getName())
                                .position(option.getPosition())
                                .bio(option.getBio())
                                .photoUrl(option.getPhotoUrl())
                                .displayOrder(option.getDisplayOrder())
                                .build();
        }

        public ElectionResponse mapElectionToResponse(Election election) {
                List<UserVoteResponse> userVotes = null;
                try {
                        User currentUser = getCurrentUser();
                        if (currentUser != null) {
                                userVotes = voteRepository
                                                .findByElection_IdAndUser_Id(election.getId(), currentUser.getId())
                                                .stream()
                                                .filter(v -> v.getVoteWeight() > 0) // Only show votes with weight > 0
                                                .map(v -> UserVoteResponse.builder()
                                                                .votingOptionId(v.getVotingOption().getId())
                                                                .votingOptionName(v.getVotingOption().getName())
                                                                .voteWeight(v.getVoteWeight())
                                                                .votedAt(v.getVotedAt())
                                                                .build())
                                                .collect(Collectors.toList());
                        }
                } catch (Exception e) {
                        // User not authenticated or other security context issue, ignore
                }

                return ElectionResponse.builder()
                                .id(election.getId())
                                .title(election.getTitle())
                                .description(election.getDescription())
                                .electionType(election.getElectionType())
                                .displayOrder(election.getDisplayOrder())
                                .votingOptions(election.getVotingOptions().stream()
                                                .map(this::mapVotingOptionToResponse)
                                                .collect(Collectors.toList()))
                                .userVotes(userVotes)
                                .build();
        }
}
