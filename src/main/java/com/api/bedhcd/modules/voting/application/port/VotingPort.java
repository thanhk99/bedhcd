package com.api.bedhcd.modules.voting.application.port;

public interface VotingPort {
    long countResolutions();
    long countVotes();
    long countResolutionsByMeetingId(String meetingId);
    long countVotesByMeetingId(String meetingId);
}
