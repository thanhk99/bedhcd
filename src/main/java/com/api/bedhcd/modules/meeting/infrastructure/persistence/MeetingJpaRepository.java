package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MeetingJpaRepository extends JpaRepository<MeetingEntity, String> {
    Optional<MeetingEntity> findByMeetingCode(String meetingCode);

    List<MeetingEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
