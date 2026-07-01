package com.api.bedhcd.modules.participant.domain.model;

import com.api.bedhcd.shared.domain.enums.ImportJobStatus;
import com.api.bedhcd.shared.domain.enums.ImportJobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob {
    private String id;
    private String meetingId;
    private ImportJobType type;
    private ImportJobStatus status;
    private long totalRows;
    private long processedRows;
    private long failedRows;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public void start() {
        this.status = ImportJobStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ImportJobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String error) {
        this.status = ImportJobStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = LocalDateTime.now();
    }

    public void incrementProcessed(long count) {
        this.processedRows += count;
    }

    public void incrementFailed(long count) {
        this.failedRows += count;
    }
}
