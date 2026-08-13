package com.api.bedhcd.modules.participant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpectedAttendanceJpaRepository extends JpaRepository<ExpectedAttendanceEntity, Long> {
    List<ExpectedAttendanceEntity> findByMeetingId(String meetingId);

    void deleteByMeetingId(String meetingId);

    long countByMeetingId(String meetingId);
}
