package com.api.bedhcd.modules.voting.infrastructure.persistence;

import com.api.bedhcd.modules.election.infrastructure.persistence.ElectionEntity;
import com.api.bedhcd.modules.resolution.infrastructure.persistence.ResolutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VoteJpaRepository extends JpaRepository<VoteEntity, Long> {
    List<VoteEntity> findByResolutionIdAndUserId(String resolutionId, String userId);
    List<VoteEntity> findByResolutionId(String resolutionId);
    List<VoteEntity> findByElectionId(String electionId);
    List<VoteEntity> findByUserIdOrderByVotedAtDesc(String userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM VoteEntity v WHERE v.resolutionId IN (SELECT r.id FROM ResolutionEntity r WHERE r.meetingId = :meetingId)")
    long countByMeetingId(@org.springframework.data.repository.query.Param("meetingId") String meetingId);

    // Xoá vote theo userId và meeting (bầu cử)
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM VoteEntity v WHERE v.electionId IN " +
           "(SELECT e.id FROM ElectionEntity e WHERE e.meetingId = :meetingId) " +
           "AND v.userId = :userId")
    void deleteByMeetingIdAndUserIdForElection(@Param("meetingId") String meetingId,
                                               @Param("userId") String userId);

    // Xoá vote theo userId và meeting (biểu quyết)
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM VoteEntity v WHERE v.resolutionId IN " +
           "(SELECT r.id FROM ResolutionEntity r WHERE r.meetingId = :meetingId) " +
           "AND v.userId = :userId")
    void deleteByMeetingIdAndUserIdForResolution(@Param("meetingId") String meetingId,
                                                 @Param("userId") String userId);
}
