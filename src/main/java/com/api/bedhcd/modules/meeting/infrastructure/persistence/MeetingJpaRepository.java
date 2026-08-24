package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface MeetingJpaRepository extends JpaRepository<MeetingEntity, String> {
    Optional<MeetingEntity> findByMeetingCode(String meetingCode);

    List<MeetingEntity> findAllByOrderByCreatedAtDesc();

    @Query("SELECT m.status, COUNT(m) FROM MeetingEntity m GROUP BY m.status")
    List<Object[]> countMeetingsGroupedByStatus();
}
