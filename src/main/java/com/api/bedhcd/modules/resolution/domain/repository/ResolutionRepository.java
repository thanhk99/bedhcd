package com.api.bedhcd.modules.resolution.domain.repository;

import com.api.bedhcd.modules.resolution.domain.model.Resolution;
import java.util.Optional;
import java.util.List;

public interface ResolutionRepository {
    Optional<Resolution> findById(String id);
    List<Resolution> findByMeetingId(String meetingId);
    Resolution save(Resolution resolution);
    void deleteById(String id);
    long count();
    long countByMeetingId(String meetingId);
    boolean existsByMeetingIdAndDisplayOrder(String meetingId, int displayOrder);
}
