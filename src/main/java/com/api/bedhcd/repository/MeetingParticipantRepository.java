package com.api.bedhcd.repository;

import com.api.bedhcd.entity.MeetingParticipant;
import com.api.bedhcd.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    List<MeetingParticipant> findByMeeting_Id(String meetingId);

    List<MeetingParticipant> findByMeetingId(String meetingId);

    Optional<MeetingParticipant> findByMeeting_IdAndUser_Id(String meetingId, String userId);

    List<MeetingParticipant> findByMeeting_IdAndStatus(String meetingId, ParticipantStatus status);

    long countByMeeting_IdAndStatus(String meetingId, ParticipantStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(mp.attendingShares + mp.receivedProxyShares), 0) FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.status = com.api.bedhcd.entity.enums.ParticipantStatus.CHECKED_IN")
    long sumTotalAttendingShares(String meetingId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(mp.sharesOwned), 0) FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId")
    long sumTotalSharesExpected(String meetingId);
}
