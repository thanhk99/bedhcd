package com.api.bedhcd.modules.participant.domain.repository;

import com.api.bedhcd.modules.participant.domain.model.ExpectedAttendance;

import java.util.List;

public interface ExpectedAttendanceRepository {
    List<ExpectedAttendance> findByMeetingId(String meetingId);

    List<ExpectedAttendance> saveAll(List<ExpectedAttendance> items);

    void deleteByMeetingId(String meetingId);

    long countByMeetingId(String meetingId);
}
