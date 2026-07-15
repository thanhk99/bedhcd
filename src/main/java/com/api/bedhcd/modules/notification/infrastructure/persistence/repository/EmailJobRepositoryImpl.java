package com.api.bedhcd.modules.notification.infrastructure.persistence.repository;

import com.api.bedhcd.modules.notification.domain.model.EmailJob;
import com.api.bedhcd.modules.notification.domain.repository.EmailJobRepository;
import com.api.bedhcd.modules.notification.infrastructure.persistence.entity.EmailJobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailJobRepositoryImpl implements EmailJobRepository {

    private final EmailJobJpaRepository jpaRepository;

    @Override
    public EmailJob save(EmailJob emailJob) {
        EmailJobEntity entity = toEntity(emailJob);
        EmailJobEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<EmailJob> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private EmailJobEntity toEntity(EmailJob domain) {
        if (domain == null) return null;
        return EmailJobEntity.builder()
                .id(domain.getId())
                .toEmail(domain.getToEmail())
                .subject(domain.getSubject())
                .body(domain.getBody())
                .status(domain.getStatus())
                .retryCount(domain.getRetryCount())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getSentAt())
                .build();
    }

    private EmailJob toDomain(EmailJobEntity entity) {
        if (entity == null) return null;
        return EmailJob.builder()
                .id(entity.getId())
                .toEmail(entity.getToEmail())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }
}
