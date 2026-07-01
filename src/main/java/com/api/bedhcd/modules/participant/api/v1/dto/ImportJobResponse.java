package com.api.bedhcd.modules.participant.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.ImportJobStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImportJobResponse {
    private String jobId;
    private String meetingId;
    private ImportJobStatus status;
    private long totalRows;
    private long processedRows;
    private long failedRows;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
