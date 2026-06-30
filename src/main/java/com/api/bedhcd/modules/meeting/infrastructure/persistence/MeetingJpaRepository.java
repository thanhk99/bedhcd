package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import java.util.Optional;

public interface MeetingJpaRepository extends JpaRepository<MeetingEntity, String> {
    Optional<MeetingEntity> findByMeetingCode(String meetingCode);

    Optional<MeetingEntity> findFirstByStatus(String status);

    long countByStatus(String status);
}
