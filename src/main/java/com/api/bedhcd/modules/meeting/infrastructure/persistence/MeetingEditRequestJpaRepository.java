package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingEditRequestJpaRepository extends JpaRepository<MeetingEditRequestEntity, String> {

    List<MeetingEditRequestEntity> findByMeetingId(String meetingId);

    List<MeetingEditRequestEntity> findByStatus(String status);

    boolean existsByMeetingIdAndStatus(String meetingId, String status);
}
