package com.api.bedhcd.repository;

import com.api.bedhcd.entity.VoteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteLogRepository extends JpaRepository<VoteLog, Long> {
    List<VoteLog> findByUserId(Long userId);

    List<VoteLog> findByVotingItem_Id(Long votingItemId);
}
