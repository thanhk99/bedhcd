package com.api.bedhcd.repository;

import com.api.bedhcd.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
        List<Vote> findByResolution_IdAndUser_Id(String resolutionId, String userId);

        List<Vote> findByUser_IdOrderByVotedAtDesc(String userId);

        List<Vote> findByResolution_Id(String resolutionId);

        long countByResolutionIdAndVotingOptionId(@Param("resolutionId") String resolutionId,
                        @Param("votingOptionId") String votingOptionId);

        List<Vote> findByElection_IdAndUser_Id(String electionId, String userId);

        List<Vote> findByElection_Id(String electionId);

        long countByElectionIdAndVotingOptionId(@Param("electionId") String electionId,
                        @Param("votingOptionId") String votingOptionId);

        void deleteAllByResolution_Id(String resolutionId);

        void deleteAllByVotingOption_Id(String votingOptionId);
}
