package com.api.bedhcd.modules.voting.domain.repository;

import com.api.bedhcd.modules.voting.domain.model.Resolution;
import java.util.Optional;
import java.util.List;

public interface ResolutionRepository {
    Optional<Resolution> findById(String id);
    List<Resolution> findByMeetingId(String meetingId);
    Resolution save(Resolution resolution);
    long count();
    long countByMeetingId(String meetingId);
}
