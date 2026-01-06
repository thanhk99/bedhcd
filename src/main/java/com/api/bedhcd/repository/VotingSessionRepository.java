package com.api.bedhcd.repository;

import com.api.bedhcd.entity.VotingSession;
import com.api.bedhcd.entity.enums.VotingSessionStatus;
import com.api.bedhcd.entity.enums.VotingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {
    List<VotingSession> findByMeetingId(Long meetingId);

    List<VotingSession> findByStatus(VotingSessionStatus status);

    List<VotingSession> findByVotingType(VotingType type);
}
