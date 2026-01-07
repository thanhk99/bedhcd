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
        List<Vote> findByVotingItem_IdAndUser_Id(Long votingItemId, Long userId);

        @Query("SELECT COUNT(v) FROM Vote v WHERE v.votingItem.id = :votingItemId AND v.candidate.id = :candidateId")
        long countByVotingItemIdAndCandidateId(@Param("votingItemId") Long votingItemId,
                        @Param("candidateId") Long candidateId);

        @Query("SELECT SUM(v.voteWeight) FROM Vote v WHERE v.votingItem.id = :votingItemId AND v.candidate.id = :candidateId")
        Long sumVoteWeightByVotingItemIdAndCandidateId(@Param("votingItemId") Long votingItemId,
                        @Param("candidateId") Long candidateId);
}
