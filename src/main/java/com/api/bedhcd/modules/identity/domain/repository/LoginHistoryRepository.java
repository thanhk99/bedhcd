package com.api.bedhcd.modules.identity.domain.repository;

import com.api.bedhcd.modules.identity.domain.model.LoginHistory;

import java.util.List;
import java.util.Optional;

public interface LoginHistoryRepository {
    void save(LoginHistory history);
    List<LoginHistory> findByUserId(String userId);
    Optional<LoginHistory> findBySessionToken(String sessionToken);
}
