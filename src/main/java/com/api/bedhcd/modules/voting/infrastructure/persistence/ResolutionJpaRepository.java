package com.api.bedhcd.modules.voting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResolutionJpaRepository extends JpaRepository<ResolutionEntity, String> {
    List<ResolutionEntity> findByMeetingId(String meetingId);

    long countByMeetingId(String meetingId);
}
