package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingConfigJpaRepository extends JpaRepository<MeetingConfigEntity, String> {
}
