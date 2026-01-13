package com.api.bedhcd.repository;

import com.api.bedhcd.entity.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectionRepository extends JpaRepository<Election, String> {
    List<Election> findByMeetingIdOrderByDisplayOrderAsc(String meetingId);

    boolean existsById(String id);

    List<Election> findByMeetingId(String meetingId);
}
