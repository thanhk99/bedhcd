package com.api.bedhcd.modules.voting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoteJpaRepository extends JpaRepository<VoteEntity, Long> {
    List<VoteEntity> findByResolutionIdAndUserId(String resolutionId, String userId);
    List<VoteEntity> findByResolutionId(String resolutionId);
    List<VoteEntity> findByUserIdOrderByVotedAtDesc(String userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM VoteEntity v WHERE v.resolutionId IN (SELECT r.id FROM ResolutionEntity r WHERE r.meetingId = :meetingId)")
    long countByMeetingId(@org.springframework.data.repository.query.Param("meetingId") String meetingId);
}
