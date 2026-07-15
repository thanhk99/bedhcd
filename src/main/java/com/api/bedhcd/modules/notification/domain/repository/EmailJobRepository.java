package com.api.bedhcd.modules.notification.domain.repository;

import com.api.bedhcd.modules.notification.domain.model.EmailJob;

import java.util.Optional;

public interface EmailJobRepository {
    EmailJob save(EmailJob emailJob);
    Optional<EmailJob> findById(String id);
}
