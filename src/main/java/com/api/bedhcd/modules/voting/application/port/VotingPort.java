package com.api.bedhcd.modules.voting.application.port;

import java.util.List;

public interface VotingPort {
    long countVotes();
    long countVotesByMeetingId(String meetingId);
    
    void submitVotes(String targetId, String userId, List<OptionVote> votes);
    void deleteVotesByTarget(String targetId, String userId);

    /**
     * Xoá toàn bộ phiếu bầu của một user trong một meeting.
     * Dùng khi quyền biểu quyết thay đổi và cần bỏ phiếu lại.
     */
    void deleteVotesByMeetingAndUser(String meetingId, String userId);
    List<VoteResult> getVotesByTarget(String targetId);
    List<com.api.bedhcd.modules.voting.domain.model.Vote> getVotesByUser(String userId);
    long countVotersByTarget(String targetId);
}
