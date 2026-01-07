package com.api.bedhcd.repository;

import com.api.bedhcd.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByVotingItem_Id(Long votingItemId);

    List<Candidate> findByVotingItem_IdOrderByDisplayOrder(Long votingItemId);
}
