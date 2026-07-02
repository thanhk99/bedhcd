package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.domain.exception.ParticipantException;
import com.api.bedhcd.modules.participant.domain.model.ImportJob;
import com.api.bedhcd.modules.participant.domain.repository.ImportJobRepository;
import com.api.bedhcd.shared.domain.enums.ImportJobStatus;
import com.api.bedhcd.shared.domain.enums.ImportJobType;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import com.api.bedhcd.shared.domain.UuidFactory;

@Service
@RequiredArgsConstructor
public class ImportApplicationService {

    private final MeetingPort meetingPort;
    private final ImportJobRepository importJobRepository;
    private final AsyncImportService asyncImportService;

    @Value("${app.import.temp-dir}")
    private String tempDirPath;

    public String importShareholders(String meetingId, MultipartFile file) {
        if (!meetingPort.canImportShareholder(meetingId)) {
            throw ParticipantException.invalidState(
                    "Trạng thái hiện tại của cuộc họp không cho phép import cổ đông.");
        }

        String filePath = saveTempFile(file);

        ImportJob job = ImportJob.builder()
                .id(UuidFactory.generate())
                .meetingId(meetingId)
                .type(ImportJobType.SHAREHOLDER)
                .status(ImportJobStatus.PENDING)
                .processedRows(0)
                .failedRows(0)
                .createdAt(LocalDateTime.now())
                .build();

        job = importJobRepository.save(job);

        asyncImportService.processShareholderImport(job.getId(), filePath);

        return job.getId();
    }

    public String importProxies(String meetingId, MultipartFile file) {
        if (!meetingPort.canRegisterProxy(meetingId)) {
            throw ParticipantException.invalidState(
                    "Trạng thái hiện tại của cuộc họp không cho phép import ủy quyền.");
        }

        String filePath = saveTempFile(file);

        ImportJob job = ImportJob.builder()
                .id(UuidFactory.generate())
                .meetingId(meetingId)
                .type(ImportJobType.PROXY)
                .status(ImportJobStatus.PENDING)
                .processedRows(0)
                .failedRows(0)
                .createdAt(LocalDateTime.now())
                .build();

        job = importJobRepository.save(job);

        asyncImportService.processProxyImport(job.getId(), filePath);

        return job.getId();
    }

    private String saveTempFile(MultipartFile file) {
        try {
            Path tempDir = Paths.get(tempDirPath);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = tempDir.resolve(fileName);
            file.transferTo(filePath.toFile());
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file tạm: " + e.getMessage());
        }
    }
}
