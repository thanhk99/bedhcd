package com.api.bedhcd.repository;

import com.api.bedhcd.entity.NotificationLog;
import com.api.bedhcd.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUser_Id(String userId);

    List<NotificationLog> findByStatus(NotificationStatus status);
}
