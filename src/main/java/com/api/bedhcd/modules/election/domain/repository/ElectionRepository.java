package com.api.bedhcd.modules.election.domain.repository;

import com.api.bedhcd.modules.election.domain.model.Election;
import java.util.Optional;
import java.util.List;

public interface ElectionRepository {
    Optional<Election> findById(String id);
    List<Election> findByMeetingId(String meetingId);
    Election save(Election election);
    void delete(Election election);
}
