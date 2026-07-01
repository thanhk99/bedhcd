package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.participant.api.v1.dto.ImportJobResponse;
import com.api.bedhcd.modules.participant.domain.model.ImportJob;
import com.api.bedhcd.modules.participant.domain.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImportJobApplicationService {
    
    private final ImportJobRepository importJobRepository;

    public ImportJobResponse getJobStatus(String jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tiến trình import: " + jobId));
                
        return ImportJobResponse.builder()
                .jobId(job.getId())
                .meetingId(job.getMeetingId())
                .status(job.getStatus())
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .failedRows(job.getFailedRows())
                .errorMessage(job.getErrorMessage())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
