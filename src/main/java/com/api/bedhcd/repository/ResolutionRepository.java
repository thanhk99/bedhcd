package com.api.bedhcd.repository;

import com.api.bedhcd.entity.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResolutionRepository extends JpaRepository<Resolution, String> {
    List<Resolution> findByMeetingIdOrderByDisplayOrderAsc(String meetingId);
}
