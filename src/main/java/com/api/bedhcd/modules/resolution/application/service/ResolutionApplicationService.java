package com.api.bedhcd.modules.resolution.application.service;

import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.resolution.domain.exception.ResolutionException;
import com.api.bedhcd.modules.resolution.domain.model.Resolution;
import com.api.bedhcd.modules.resolution.domain.repository.ResolutionRepository;
import com.api.bedhcd.modules.voting.api.v1.dto.ResolutionRequest;
import com.api.bedhcd.modules.voting.api.v1.dto.ResolutionResponse;
import com.api.bedhcd.modules.resolution.application.mapper.ResolutionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.voting.api.v1.dto.VoteRequest;
import com.api.bedhcd.modules.voting.api.v1.dto.VotingResultResponse;
import com.api.bedhcd.modules.voting.application.port.OptionVote;
import com.api.bedhcd.modules.voting.application.port.VoteResult;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.modules.resolution.domain.model.VotingOption;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResolutionApplicationService {

    private final ResolutionRepository resolutionRepository;
    private final MeetingPort meetingPort;
    private final IdentityPort identityPort;
    private final ParticipantPort participantPort;
    private final VotingPort votingPort;
    private final ResolutionMapper resolutionMapper;

    @Transactional(readOnly = true)
    public List<ResolutionResponse> listResolutionsByMeetingId(String meetingId) {
        if (meetingPort.getStatus(meetingId) == null) {
            throw ResolutionException.notFound("Cuộc họp", meetingId);
        }

        return resolutionRepository.findByMeetingId(meetingId).stream()
                .map(resolutionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResolutionResponse getById(String id) {
        return resolutionRepository.findById(id)
                .map(resolutionMapper::toResponse)
                .orElseThrow(() -> ResolutionException.notFound("Nghị quyết", id));
    }

    @Transactional
    public ResolutionResponse createResolution(String meetingId, ResolutionRequest request) {
        Resolution resolution = Resolution.createNew(
                meetingId,
                request.getTitle(),
                request.getDescription(),
                request.getDisplayOrder());
        return resolutionMapper.toResponse(resolutionRepository.save(resolution));
    }

    @Transactional
    public ResolutionResponse updateResolution(String meetingId, String id, ResolutionRequest request) {
        Resolution resolution = resolutionRepository.findById(id)
                .orElseThrow(() -> ResolutionException.notFound("Nghị quyết", id));

        resolution.setTitle(request.getTitle());
        resolution.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) {
            resolution.setDisplayOrder(request.getDisplayOrder());
        }

        return resolutionMapper.toResponse(resolutionRepository.save(resolution));
    }

    @Transactional
    public void deleteResolution(String meetingId, String id) {
        Resolution resolution = resolutionRepository.findById(id)
                .orElseThrow(() -> ResolutionException.notFound("Nghị quyết", id));

        resolutionRepository.deleteById(id);
    }

    @Transactional
    public void submitVote(String resolutionId, VoteRequest request) {
        String userId = identityPort.getCurrentUserId();
        if (userId == null) throw ResolutionException.unauthorized();

        Resolution resolution = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> ResolutionException.notFound("Nghị quyết", resolutionId));

        if (!participantPort.isCheckedIn(resolution.getMeetingId(), userId)) {
            throw ResolutionException.invalidState("Cổ đông chưa điểm danh, không thể bỏ phiếu.");
        }

        long votingPower = participantPort.getVotingPower(resolution.getMeetingId(), userId);
        
        List<OptionVote> optionVotes = request.getOptionVotes().stream()
            .filter(opt -> resolution.findOption(opt.getVotingOptionId()) != null)
            .map(opt -> OptionVote.builder()
                .optionId(opt.getVotingOptionId())
                .weight(votingPower)
                .build())
            .collect(Collectors.toList());

        votingPort.submitVotes(resolutionId, userId, optionVotes);
    }

    @Transactional(readOnly = true)
    public VotingResultResponse getResults(String resolutionId) {
        Resolution resolution = resolutionRepository.findById(resolutionId)
                .orElseThrow(() -> ResolutionException.notFound("Nghị quyết", resolutionId));
        
        List<VoteResult> portResults = votingPort.getVotesByTarget(resolutionId);
        
        List<VotingOption> options = resolution.getOptions() != null ? resolution.getOptions() : java.util.Collections.emptyList();
        
        List<VotingResultResponse.VotingOptionResult> optionResults = options.stream()
                .map(opt -> {
                    VoteResult pr = portResults.stream().filter(r -> r.getOptionId().equals(opt.getId())).findFirst().orElse(null);
                    long weight = pr != null ? pr.getTotalWeight() : 0;
                    long count = pr != null ? pr.getVoteCount() : 0;
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
                .totalCollected(votingPort.countVotersByTarget(resolutionId))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
