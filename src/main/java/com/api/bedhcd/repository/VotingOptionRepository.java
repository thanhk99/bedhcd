package com.api.bedhcd.repository;

import com.api.bedhcd.entity.VotingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotingOptionRepository extends JpaRepository<VotingOption, String> {
    List<VotingOption> findByResolution_Id(String resolutionId);

    List<VotingOption> findByResolution_IdOrderByDisplayOrder(String resolutionId);

    List<VotingOption> findByElection_Id(String electionId);

    List<VotingOption> findByElection_IdOrderByDisplayOrder(String electionId);

    void deleteAllByResolution_Id(String resolutionId);

    boolean existsById(String id);

}
