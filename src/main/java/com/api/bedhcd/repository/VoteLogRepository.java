package com.api.bedhcd.repository;

import com.api.bedhcd.entity.VoteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteLogRepository extends JpaRepository<VoteLog, Long> {
    List<VoteLog> findByUser_Id(String userId);

    List<VoteLog> findByResolution_Id(String resolutionId);

    List<VoteLog> findByElection_Id(String electionId);
}
