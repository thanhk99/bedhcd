package com.api.bedhcd.modules.voting.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.voting.api.v1.dto.*;
import com.api.bedhcd.modules.voting.application.mapper.VotingMapper;
import com.api.bedhcd.modules.voting.domain.exception.VotingException;
import com.api.bedhcd.modules.voting.domain.model.Resolution;
import com.api.bedhcd.modules.voting.domain.model.Vote;
import com.api.bedhcd.modules.voting.domain.model.VotingOption;
import com.api.bedhcd.modules.voting.domain.repository.ResolutionRepository;
import com.api.bedhcd.modules.voting.domain.repository.VoteRepository;
import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VotingApplicationService {

    private final ResolutionRepository resolutionRepository;
    private final VoteRepository voteRepository;
    private final VotingMapper votingMapper;
    
    private final IdentityPort identityPort;
    private final MeetingPort meetingPort;
    private final ParticipantPort participantPort;

    @Transactional(readOnly = true)
    public List<ResolutionResponse> getByMeeting(String meetingId) {
        return resolutionRepository.findByMeetingId(meetingId).stream()
                .map(votingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResolutionResponse getById(String id) {
        return resolutionRepository.findById(id)
                .map(votingMapper::toResponse)
                .orElseThrow(() -> VotingException.notFound("Nghị quyết", id));
    }

    @Transactional
    public ResolutionResponse createResolution(String meetingId, ResolutionRequest request) {
        Resolution resolution = Resolution.createNew(
                meetingId,
                request.getTitle(),
                request.getDescription(),
                request.getDisplayOrder());
        return votingMapper.toResponse(resolutionRepository.save(resolution));
    }

    @Transactional
    public void submitVote(String resolutionId, VoteRequest request) {
        String userId = identityPort.getCurrentUserId();
        if (userId == null) throw VotingException.unauthorized();

        Resolution resolution = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> VotingException.notFound("Nghị quyết", resolutionId));

        if (!participantPort.isCheckedIn(resolution.getMeetingId(), userId)) {
            throw VotingException.invalidState("Cổ đông chưa điểm danh, không thể bỏ phiếu.");
        }

        long votingPower = participantPort.getVotingPower(resolution.getMeetingId(), userId);
        
        // Xoá phiếu cũ nếu có
        List<Vote> oldVotes = voteRepository.findByResolutionAndUser(resolutionId, userId);
        oldVotes.forEach(v -> voteRepository.delete(v.getId()));

        // Lưu phiếu mới
        for (VoteRequest.OptionVoteRequest optVote : request.getOptionVotes()) {
            VotingOption option = resolution.findOption(optVote.getVotingOptionId());
            if (option == null) continue;

            Vote vote = Vote.builder()
                    .resolutionId(resolutionId)
                    .votingOptionId(optVote.getVotingOptionId())
                    .userId(userId)
                    .voteWeight(votingPower)
                    .votedAt(LocalDateTime.now())
                    .build();
            voteRepository.save(vote);
        }
    }

    @Transactional
    public void submitBatchVotes(String meetingId, BatchVoteRequest request) {
        for (BatchVoteRequest.ItemVote item : request.getItems()) {
            submitVote(item.getItemId(), item.getVoteRequest());
        }
    }

    @Transactional(readOnly = true)
    public VotingResultResponse getResults(String resolutionId) {
        Resolution resolution = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> VotingException.notFound("Nghị quyết", resolutionId));
        
        List<Vote> votes = voteRepository.findByResolution(resolutionId);
        
        List<VotingOption> options = resolution.getOptions() != null ? resolution.getOptions() : java.util.Collections.emptyList();
        
        List<VotingResultResponse.VotingOptionResult> optionResults = options.stream()
                .map(opt -> {
                    long weight = votes.stream()
                            .filter(v -> v.getVotingOptionId().equals(opt.getId()))
                            .mapToLong(Vote::getVoteWeight)
                            .sum();
                    long count = votes.stream()
                            .filter(v -> v.getVotingOptionId().equals(opt.getId()))
                            .count();
                    return VotingResultResponse.VotingOptionResult.builder()
                            .votingOptionId(opt.getId())
                            .votingOptionName(opt.getName())
                            .voteCount(count)
                            .totalWeight(weight)
                            .build();
                }).collect(Collectors.toList());

        long totalWeight = optionResults.stream().mapToLong(VotingResultResponse.VotingOptionResult::getTotalWeight).sum();
        
        // Tính %
        if (totalWeight > 0) {
            for (VotingResultResponse.VotingOptionResult res : optionResults) {
                res.setPercentage((double) res.getTotalWeight() * 100 / totalWeight);
            }
        }

        return VotingResultResponse.builder()
                .meetingId(resolution.getMeetingId())
                .resolutionId(resolutionId)
                .resolutionTitle(resolution.getTitle())
                .results(optionResults)
                .totalWeight(totalWeight)
                .totalCollected(votes.stream().map(Vote::getUserId).distinct().count())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
