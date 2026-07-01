package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.modules.participant.domain.model.ImportJob;
import com.api.bedhcd.modules.participant.domain.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ImportJobRepositoryImpl implements ImportJobRepository {

    private final ImportJobJpaRepository jpaRepository;

    @Override
    public ImportJob save(ImportJob importJob) {
        ImportJobEntity entity = toEntity(importJob);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<ImportJob> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private ImportJobEntity toEntity(ImportJob domain) {
        return ImportJobEntity.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .type(domain.getType())
                .status(domain.getStatus())
                .totalRows(domain.getTotalRows())
                .processedRows(domain.getProcessedRows())
                .failedRows(domain.getFailedRows())
                .errorMessage(domain.getErrorMessage())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private ImportJob toDomain(ImportJobEntity entity) {
        return ImportJob.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .type(entity.getType())
                .status(entity.getStatus())
                .totalRows(entity.getTotalRows())
                .processedRows(entity.getProcessedRows())
                .failedRows(entity.getFailedRows())
                .errorMessage(entity.getErrorMessage())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
