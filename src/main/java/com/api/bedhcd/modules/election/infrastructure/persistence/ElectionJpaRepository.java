package com.api.bedhcd.modules.election.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ElectionJpaRepository extends JpaRepository<ElectionEntity, String> {
    List<ElectionEntity> findByMeetingId(String meetingId);
}
