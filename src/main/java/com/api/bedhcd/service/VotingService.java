package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.ResolutionRequest;
import com.api.bedhcd.dto.request.VotingOptionRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.response.VotingOptionResponse;
import com.api.bedhcd.dto.response.ResolutionResponse;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class VotingService {

        private final ResolutionRepository resolutionRepository;
        private final VotingOptionRepository votingOptionRepository;
        private final VoteRepository voteRepository;
        private final VoteDraftRepository voteDraftRepository;
        private final MeetingRepository meetingRepository;
        private final UserRepository userRepository;
        private final VoteLogRepository voteLogRepository;
        private final MeetingParticipantRepository meetingParticipantRepository;

        // Semantic IDs for default resolution voting options
        private static final Map<String, String> RESOLUTION_OPTION_IDS = Map.of(
                        "Đồng ý", "yes",
                        "Không đồng ý", "no",
                        "Không ý kiến", "not_agree");

        @Transactional
        public ResolutionResponse createResolution(String meetingId, ResolutionRequest request) {
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                Resolution resolution = Resolution.builder()
                                .id(RandomUtil.generate6DigitId(resolutionRepository::existsById))
                                .meeting(meeting)
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                                .build();

                resolution = resolutionRepository.save(resolution);

                // Tự động tạo 3 lựa chọn cố định cho nghị quyết
                createVotingOption(resolution, "Đồng ý", 1);
                createVotingOption(resolution, "Không đồng ý", 2);
                createVotingOption(resolution, "Không ý kiến", 3);

                return mapResolutionToResponse(resolution);
        }

        @Transactional
        public ResolutionResponse updateResolution(String resolutionId, ResolutionRequest request) {
                Resolution resolution = resolutionRepository.findById(resolutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));

                resolution.setTitle(request.getTitle());
                resolution.setDescription(request.getDescription());
                if (request.getDisplayOrder() != null) {
                        resolution.setDisplayOrder(request.getDisplayOrder());
                }

                resolution = resolutionRepository.save(resolution);
                return mapResolutionToResponse(resolution);
        }

        @Transactional
        public void deleteResolution(String resolutionId) {
                Resolution resolution = resolutionRepository.findById(resolutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));

                // Xóa các dữ liệu liên quan
                voteRepository.deleteAllByResolution_Id(resolutionId);
                voteDraftRepository.deleteByResolution_Id(resolutionId);
                votingOptionRepository.deleteAllByResolution_Id(resolutionId);

                resolutionRepository.delete(resolution);
        }

        @Transactional
        public void deleteVotingOption(String optionId) {
                VotingOption option = votingOptionRepository.findById(optionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Voting option not found"));

                // Không cho phép xóa các lựa chọn mặc định của Nghị quyết
                if (option.getResolution() != null && isDefaultResolutionOption(option.getName())) {
                        throw new BadRequestException("Cannot delete default resolution options: " + option.getName());
                }

                // Xóa các dữ liệu liên quan
                voteRepository.deleteAllByVotingOption_Id(optionId);
                voteDraftRepository.deleteByVotingOption_Id(optionId);

                votingOptionRepository.delete(option);
        }

        private boolean isDefaultResolutionOption(String name) {
                return name.equals("Đồng ý") || name.equals("Không đồng ý") || name.equals("Không ý kiến");
        }

        private void createVotingOption(Resolution resolution, String name, int order) {
                // Use semantic ID for default options, random ID for custom options
                String id = RESOLUTION_OPTION_IDS.getOrDefault(name,
                                RandomUtil.generate6DigitId(votingOptionRepository::existsById));

                VotingOption option = VotingOption.builder()
                                .id(id)
                                .resolution(resolution)
                                .name(name)
                                .displayOrder(order)
                                .build();
                votingOptionRepository.save(option);
        }

        public ResolutionResponse getResolutionById(String resolutionId) {
                Resolution resolution = resolutionRepository.findById(resolutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));
                return mapResolutionToResponse(resolution);
        }

        public VotingOptionResponse getVotingOptionById(String votingOptionId) {
                VotingOption option = votingOptionRepository.findById(votingOptionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Voting option not found"));
                return mapVotingOptionToResponse(option);
        }

        @Transactional
        public VotingOptionResponse updateVotingOption(String id, VotingOptionRequest request) {
                VotingOption option = votingOptionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Voting option not found"));

                option.setName(request.getName());
                option.setPosition(request.getPosition());
                option.setBio(request.getBio());
                option.setPhotoUrl(request.getPhotoUrl());
                if (request.getDisplayOrder() != null) {
                        option.setDisplayOrder(request.getDisplayOrder());
                }

                option = votingOptionRepository.save(option);
                return mapVotingOptionToResponse(option);
        }

        @Transactional
        public void castVote(String resolutionId, VoteRequest request) {
                Resolution resolution = resolutionRepository.findById(resolutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));

                User currentUser = getCurrentUser();
                long votingPower = calculateVotingPower(currentUser.getId(), resolution.getMeeting().getId());

                // Validation: Chỉ được chọn 1 lựa chọn
                if (request.getOptionVotes().size() > 1) {
                        throw new BadRequestException("Can only vote for one option in resolution");
                }

                // Get existing votes
                List<Vote> existingVotes = voteRepository.findByResolution_IdAndUser_Id(resolutionId,
                                currentUser.getId());
                Map<String, Vote> existingVoteMap = existingVotes.stream()
                                .collect(Collectors.toMap(v -> v.getVotingOption().getId(), v -> v));

                // Delete drafts
                voteDraftRepository.deleteByResolution_IdAndUser_Id(resolutionId, currentUser.getId());

                LocalDateTime now = LocalDateTime.now();
                VoteRequest.OptionVoteRequest optionVote = request.getOptionVotes().get(0);
                String selectedOptionId = optionVote.getVotingOptionId();

                VotingOption option = votingOptionRepository.findById(selectedOptionId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Voting option not found: " + selectedOptionId));

                Vote vote;
                VoteAction action;

                // Check if vote exists for this option
                if (existingVoteMap.containsKey(selectedOptionId)) {
                        // UPDATE existing vote
                        vote = existingVoteMap.get(selectedOptionId);
                        vote.setVoteWeight(votingPower);
                        vote.setVotedAt(now);
                        action = VoteAction.VOTE_CHANGED;
                } else {
                        // INSERT new vote
                        vote = Vote.builder()
                                        .resolution(resolution)
                                        .user(currentUser)
                                        .votingOption(option)
                                        .voteWeight(votingPower)
                                        .votedAt(now)
                                        .build();
                        action = VoteAction.VOTE_CAST;
                }

                vote = voteRepository.save(vote);

                // Log the action
                VoteLog log = VoteLog.builder()
                                .user(currentUser)
                                .resolution(resolution)
                                .vote(vote)
                                .action(action)
                                .votingOption(option)
                                .build();
                voteLogRepository.save(log);

                // Set weight = 0 for options no longer selected
                for (Vote oldVote : existingVotes) {
                        String oldOptionId = oldVote.getVotingOption().getId();
                        if (!oldOptionId.equals(selectedOptionId) && oldVote.getVoteWeight() > 0) {
                                oldVote.setVoteWeight(0L);
                                oldVote.setVotedAt(now);
                                voteRepository.save(oldVote);

                                // Log VOTE_CHANGED with weight 0
                                VoteLog zeroLog = VoteLog.builder()
                                                .user(currentUser)
                                                .resolution(resolution)
                                                .vote(oldVote)
                                                .action(VoteAction.VOTE_CHANGED)
                                                .votingOption(oldVote.getVotingOption())
                                                .build();
                                voteLogRepository.save(zeroLog);
                        }
                }
        }

        @Transactional
        public void saveDraft(String resolutionId, VoteRequest request) {
                Resolution resolution = resolutionRepository.findById(resolutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));

                User currentUser = getCurrentUser();
                voteDraftRepository.deleteByResolution_IdAndUser_Id(resolutionId, currentUser.getId());

                for (VoteRequest.OptionVoteRequest optionVote : request.getOptionVotes()) {
                        VotingOption option = votingOptionRepository.findById(optionVote.getVotingOptionId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Voting option not found"));

                        VoteDraft draft = VoteDraft.builder()
                                        .resolution(resolution)
                                        .user(currentUser)
                                        .votingOption(option)
                                        .build();

                        voteDraftRepository.save(draft);
                }
        }

        public VotingResultResponse getVotingResults(String resolutionId) {
                Resolution resolution = resolutionRepository.findById(resolutionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));

                List<Vote> votes = voteRepository.findByResolution_Id(resolutionId);
                List<VotingOption> options = votingOptionRepository
                                .findByResolution_IdOrderByDisplayOrder(resolutionId);

                // Only count votes with weight > 0
                long totalVoters = votes.stream()
                                .filter(v -> v.getVoteWeight() > 0)
                                .map(v -> v.getUser().getId())
                                .distinct()
                                .count();

                List<VotingResultResponse.VotingOptionResult> results = options.stream()
                                .map(option -> {
                                        long totalOptionWeight = votes.stream()
                                                        .filter(v -> v.getVoteWeight() > 0) // Only count active votes
                                                        .filter(v -> v.getVotingOption() != null && v.getVotingOption()
                                                                        .getId().equals(option.getId()))
                                                        .mapToLong(Vote::getVoteWeight)
                                                        .sum();

                                        long votersForOption = votes.stream()
                                                        .filter(v -> v.getVoteWeight() > 0) // Only count active votes
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
                                .meetingId(resolution.getMeeting().getId())
                                .meetingTitle(resolution.getMeeting().getTitle())
                                .resolutionId(resolution.getId())
                                .resolutionTitle(resolution.getTitle())
                                .results(results)
                                .totalVoters(totalVoters)
                                .totalWeight(totalWeight)
                                .createdAt(LocalDateTime.now())
                                .build();
        }

        private long calculateVotingPower(String userId, String meetingId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                MeetingParticipant participant = getOrCreateParticipant(meeting, user);

                long baseShares = participant.getSharesOwned() != null ? participant.getSharesOwned() : 0;
                long receivedShares = participant.getReceivedProxyShares() != null
                                ? participant.getReceivedProxyShares()
                                : 0;

                return baseShares + receivedShares;
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

        public ResolutionResponse mapResolutionToResponse(Resolution resolution) {
                List<VotingOption> options = votingOptionRepository
                                .findByResolution_IdOrderByDisplayOrder(resolution.getId());

                List<UserVoteResponse> userVotes = null;
                try {
                        User currentUser = getCurrentUser();
                        if (currentUser != null) {
                                userVotes = voteRepository
                                                .findByResolution_IdAndUser_Id(resolution.getId(), currentUser.getId())
                                                .stream()
                                                .filter(v -> v.getVoteWeight() > 0)
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

                return ResolutionResponse.builder()
                                .id(resolution.getId())
                                .title(resolution.getTitle())
                                .description(resolution.getDescription())
                                .displayOrder(resolution.getDisplayOrder())
                                .votingOptions(options.stream()
                                                .map(this::mapVotingOptionToResponse)
                                                .collect(Collectors.toList()))
                                .userVotes(userVotes)
                                .build();
        }
}
