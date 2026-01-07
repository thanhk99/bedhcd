package com.api.bedhcd.repository;

import com.api.bedhcd.entity.VoteDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteDraftRepository extends JpaRepository<VoteDraft, Long> {
    List<VoteDraft> findByVotingItem_IdAndUser_Id(Long votingItemId, Long userId);

    void deleteByVotingItem_IdAndUser_Id(Long votingItemId, Long userId);
}
