package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.participant.domain.model.ImportJob;
import com.api.bedhcd.modules.participant.domain.repository.ImportJobRepository;
import com.api.bedhcd.shared.dto.importing.ProxyImportRecord;
import com.api.bedhcd.shared.dto.importing.ShareholderImportRecord;
import com.api.bedhcd.util.StreamingExcelHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncImportService {

    private final ImportJobRepository importJobRepository;
    private final ImportBatchService importBatchService;

    @Async("taskExecutor")
    public void processShareholderImport(String jobId, String filePath) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        job.start();
        safeUpdateJob(job);

        try {
            StreamingExcelHelper.streamShareholders(filePath, 1000, (List<ShareholderImportRecord> batch) -> {
                try {
                    importBatchService.processShareholderBatch(job.getMeetingId(), batch);
                    job.incrementProcessed(batch.size());
                } catch (Exception e) {
                    log.error("Lỗi batch shareholder: {}", e.getMessage(), e);
                    job.incrementFailed(batch.size());
                    String currentError = job.getErrorMessage() == null ? "" : job.getErrorMessage() + "\n";
                    job.setErrorMessage(currentError + "Lỗi batch: " + e.getMessage());
                }
                safeUpdateJob(job);
            });

            job.complete();
            safeUpdateJob(job);
            log.info("Hoàn thành import cổ đông: jobId={}, processed={}", jobId, job.getProcessedRows());
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi import cổ đông: jobId={}, error={}", jobId, e.getMessage(), e);
            job.fail("Lỗi hệ thống: " + e.getMessage());
            safeUpdateJob(job);
        } finally {
            cleanupFile(filePath);
        }
    }

    @Async("taskExecutor")
    public void processProxyImport(String jobId, String filePath) {
        ImportJob job = importJobRepository.findById(jobId).orElseThrow();
        job.start();
        safeUpdateJob(job);

        try {
            StreamingExcelHelper.streamProxies(filePath, 1000, (List<ProxyImportRecord> batch) -> {
                try {
                    importBatchService.processProxyBatch(job.getMeetingId(), batch);
                    job.incrementProcessed(batch.size());
                } catch (Exception e) {
                    log.error("Lỗi batch proxy: {}", e.getMessage(), e);
                    job.incrementFailed(batch.size());
                    String currentError = job.getErrorMessage() == null ? "" : job.getErrorMessage() + "\n";
                    job.setErrorMessage(currentError + "Lỗi batch: " + e.getMessage());
                }
                safeUpdateJob(job);
            });

            job.complete();
            safeUpdateJob(job);
            log.info("Hoàn thành import ủy quyền: jobId={}, processed={}", jobId, job.getProcessedRows());
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi import ủy quyền: jobId={}, error={}", jobId, e.getMessage(), e);
            job.fail("Lỗi hệ thống: " + e.getMessage());
            safeUpdateJob(job);
        } finally {
            cleanupFile(filePath);
        }
    }

    /**
     * Đảm bảo trạng thái Job luôn được lưu, retry tối đa 3 lần để tránh stuck IN_PROGRESS.
     */
    private void safeUpdateJob(ImportJob job) {
        int retries = 3;
        while (retries-- > 0) {
            try {
                importJobRepository.save(job);
                return;
            } catch (Exception e) {
                log.warn("Không thể lưu trạng thái job {}, còn {} lần thử: {}", job.getId(), retries, e.getMessage());
                if (retries > 0) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        log.error("Không thể lưu trạng thái job {} sau nhiều lần thử!", job.getId());
    }


    private void cleanupFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception e) {
            log.warn("Không thể xóa file tạm: {}", filePath);
        }
    }
}

