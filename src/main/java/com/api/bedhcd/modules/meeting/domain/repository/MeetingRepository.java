package com.api.bedhcd.modules.meeting.domain.repository;

import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import java.util.Optional;

public interface MeetingRepository {
    Optional<Meeting> findById(String id);
    Optional<Meeting> findByMeetingCode(String code);
    java.util.List<Meeting> findAll();
    java.util.List<Meeting> findAllOrderByCreatedAtDesc();
    Meeting save(Meeting meeting);
    void deleteById(String id);
    long count();
    long countByStatus(String status);
}
