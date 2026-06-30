package com.api.bedhcd.modules.meeting.domain.repository;

import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import java.util.List;
import java.util.Optional;

public interface MeetingConfigRepository {
    List<MeetingConfig> findAll();
    Optional<MeetingConfig> findById(String id);
    MeetingConfig save(MeetingConfig config);
    void delete(String id);
}
