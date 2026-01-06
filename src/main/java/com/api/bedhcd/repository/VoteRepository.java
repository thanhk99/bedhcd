package com.api.bedhcd.repository;

import com.api.bedhcd.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByVotingSessionIdAndUserId(Long sessionId, Long userId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.votingSession.id = :sessionId AND v.candidate.id = :candidateId")
    long countByVotingSessionIdAndCandidateId(@Param("sessionId") Long sessionId,
            @Param("candidateId") Long candidateId);

    @Query("SELECT SUM(v.voteWeight) FROM Vote v WHERE v.votingSession.id = :sessionId AND v.candidate.id = :candidateId")
    Long sumVoteWeightByVotingSessionIdAndCandidateId(@Param("sessionId") Long sessionId,
            @Param("candidateId") Long candidateId);
}
