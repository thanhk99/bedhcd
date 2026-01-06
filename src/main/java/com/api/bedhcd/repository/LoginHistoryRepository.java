package com.api.bedhcd.repository;

import com.api.bedhcd.entity.LoginHistory;
import com.api.bedhcd.entity.enums.LoginStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserId(Long userId);

    List<LoginHistory> findByStatus(LoginStatus status);
}
