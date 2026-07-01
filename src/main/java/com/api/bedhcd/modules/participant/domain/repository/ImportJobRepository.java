package com.api.bedhcd.modules.participant.domain.repository;

import com.api.bedhcd.modules.participant.domain.model.ImportJob;

import java.util.Optional;

public interface ImportJobRepository {
    ImportJob save(ImportJob importJob);
    Optional<ImportJob> findById(String id);
}
