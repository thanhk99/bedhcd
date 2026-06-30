package com.api.bedhcd.modules.participant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParticipantJpaRepository extends JpaRepository<ParticipantEntity, Long> {
    Optional<ParticipantEntity> findByMeetingIdAndUserId(String meetingId, String userId);
    java.util.List<ParticipantEntity> findByUserId(String userId);
    java.util.List<ParticipantEntity> findByMeetingId(String meetingId);
    java.util.List<ParticipantEntity> findByMeetingIdAndStatusIn(String meetingId, java.util.List<com.api.bedhcd.shared.domain.enums.ParticipantStatus> statuses);
    long countByMeetingId(String meetingId);
    long countByMeetingIdAndStatusIn(String meetingId, java.util.List<com.api.bedhcd.shared.domain.enums.ParticipantStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.sharesOwned), 0) FROM ParticipantEntity p")
    long sumTotalShares();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.sharesOwned), 0) FROM ParticipantEntity p WHERE p.meetingId = :meetingId")
    long sumTotalSharesByMeetingId(@org.springframework.data.repository.query.Param("meetingId") String meetingId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.attendingShares + p.receivedProxyShares), 0) FROM ParticipantEntity p WHERE p.status IN :statuses")
    long sumAttendingSharesByStatusIn(@org.springframework.data.repository.query.Param("statuses") java.util.List<com.api.bedhcd.shared.domain.enums.ParticipantStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.attendingShares + p.receivedProxyShares), 0) FROM ParticipantEntity p WHERE p.meetingId = :meetingId AND p.status IN :statuses")
    long sumAttendingSharesByMeetingIdAndStatusIn(
            @org.springframework.data.repository.query.Param("meetingId") String meetingId,
            @org.springframework.data.repository.query.Param("statuses") java.util.List<com.api.bedhcd.shared.domain.enums.ParticipantStatus> statuses);
}
