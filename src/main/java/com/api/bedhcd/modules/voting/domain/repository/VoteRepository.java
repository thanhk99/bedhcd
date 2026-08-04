package com.api.bedhcd.modules.voting.domain.repository;

import com.api.bedhcd.modules.voting.domain.model.Vote;
import java.util.List;
import java.util.Optional;

public interface VoteRepository {
    List<Vote> findByResolutionAndUser(String resolutionId, String userId);
    Vote save(Vote vote);
    void delete(Long id);
    List<Vote> findByResolution(String resolutionId);
    List<Vote> findByElection(String electionId);
    List<Vote> findByUser(String userId);
    long count();
    long countByMeetingId(String meetingId);
}
