package com.api.bedhcd.repository;

import com.api.bedhcd.entity.VoteDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteDraftRepository extends JpaRepository<VoteDraft, Long> {
    List<VoteDraft> findByResolution_IdAndUser_Id(String resolutionId, String userId);

    void deleteByResolution_IdAndUser_Id(String resolutionId, String userId);

    void deleteByResolution_Id(String resolutionId);

    List<VoteDraft> findByElection_IdAndUser_Id(String electionId, String userId);

    void deleteByElection_IdAndUser_Id(String electionId, String userId);

    void deleteByVotingOption_Id(String votingOptionId);
}
