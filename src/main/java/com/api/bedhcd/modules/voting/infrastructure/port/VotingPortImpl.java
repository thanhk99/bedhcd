package com.api.bedhcd.modules.voting.infrastructure.port;

import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.modules.voting.domain.repository.ResolutionRepository;
import com.api.bedhcd.modules.voting.domain.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VotingPortImpl implements VotingPort {

    private final ResolutionRepository resolutionRepository;
    private final VoteRepository voteRepository;

    @Override
    public long countResolutions() {
        return resolutionRepository.count();
    }

    @Override
    public long countVotes() {
        return voteRepository.count();
    }

    @Override
    public long countResolutionsByMeetingId(String meetingId) {
        return resolutionRepository.countByMeetingId(meetingId);
    }

    @Override
    public long countVotesByMeetingId(String meetingId) {
        return voteRepository.countByMeetingId(meetingId);
    }
}
