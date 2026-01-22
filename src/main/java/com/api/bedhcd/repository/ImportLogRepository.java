package com.api.bedhcd.repository;

import com.api.bedhcd.entity.ImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportLogRepository extends JpaRepository<ImportLog, Long> {
    List<ImportLog> findByMeetingId(String meetingId);
}
