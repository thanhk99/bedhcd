package com.api.bedhcd.repository;

import com.api.bedhcd.entity.MeetingParticipant;
import com.api.bedhcd.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    List<MeetingParticipant> findByMeetingId(Long meetingId);

    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    long countByMeetingIdAndStatus(Long meetingId, ParticipantStatus status);
}
