package com.api.bedhcd.modules.participant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.bedhcd.shared.domain.enums.ParticipantStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipantJpaRepository extends JpaRepository<ParticipantEntity, Long> {
    Optional<ParticipantEntity> findByMeetingIdAndUserId(String meetingId, String userId);

    List<ParticipantEntity> findAllByMeetingIdAndUserIdIn(String meetingId, List<String> userIds);

    List<ParticipantEntity> findByUserId(String userId);

    List<ParticipantEntity> findByMeetingId(String meetingId);

    List<ParticipantEntity> findByMeetingIdAndStatusInOrderByCheckedInAtDesc(String meetingId,
            List<ParticipantStatus> statuses);

    long countByMeetingId(String meetingId);

    long countByMeetingIdAndStatusIn(String meetingId,
            List<ParticipantStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.sharesOwned), 0) FROM ParticipantEntity p")
    long sumTotalShares();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.sharesOwned), 0) FROM ParticipantEntity p WHERE p.meetingId = :meetingId")
    long sumTotalSharesByMeetingId(@org.springframework.data.repository.query.Param("meetingId") String meetingId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM ParticipantEntity p WHERE p.meetingId = :meetingId AND p.status IN :statuses AND p.splitTicket = false")
    long countCheckedInExcludingSplitTickets(
            @org.springframework.data.repository.query.Param("meetingId") String meetingId,
            @org.springframework.data.repository.query.Param("statuses") List<ParticipantStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.attendingShares + p.receivedProxyShares), 0) FROM ParticipantEntity p WHERE p.status IN :statuses")
    long sumAttendingSharesByStatusIn(
            @org.springframework.data.repository.query.Param("statuses") List<ParticipantStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.attendingShares + p.receivedProxyShares), 0) FROM ParticipantEntity p WHERE p.meetingId = :meetingId AND p.status IN :statuses AND p.splitTicket = false")
    long sumAttendingSharesByMeetingIdAndStatusIn(
            @org.springframework.data.repository.query.Param("meetingId") String meetingId,
            @org.springframework.data.repository.query.Param("statuses") List<ParticipantStatus> statuses);
}
