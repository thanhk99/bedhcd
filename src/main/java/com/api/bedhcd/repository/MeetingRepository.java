package com.api.bedhcd.repository;

import com.api.bedhcd.entity.Meeting;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.entity.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, String> {
    List<Meeting> findByStatus(MeetingStatus status);

    List<Meeting> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Meeting> findByCreatedBy(User user);

    boolean existsByStatus(MeetingStatus status);

    java.util.Optional<Meeting> findFirstByStatus(MeetingStatus status);

    long countByStatus(com.api.bedhcd.entity.enums.MeetingStatus status);
}
