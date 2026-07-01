package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.ImportJobStatus;
import com.api.bedhcd.shared.domain.enums.ImportJobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "meeting_id", nullable = false, length = 36)
    private String meetingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ImportJobType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ImportJobStatus status;

    @Column(name = "total_rows")
    private long totalRows;

    @Column(name = "processed_rows")
    private long processedRows;

    @Column(name = "failed_rows")
    private long failedRows;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
