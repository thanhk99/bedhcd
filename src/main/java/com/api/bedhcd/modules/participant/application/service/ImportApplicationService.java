package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.api.v1.dto.ExpectedAttendancePreviewResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.ImportExpectedResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.ReconciliationItemResponse;
import com.api.bedhcd.modules.participant.domain.exception.ParticipantException;
import com.api.bedhcd.modules.participant.domain.model.ExpectedAttendance;
import com.api.bedhcd.modules.participant.domain.model.ImportJob;
import com.api.bedhcd.modules.participant.domain.repository.ExpectedAttendanceRepository;
import com.api.bedhcd.modules.participant.domain.repository.ImportJobRepository;
import com.api.bedhcd.shared.domain.enums.ImportJobStatus;
import com.api.bedhcd.shared.domain.enums.ImportJobType;
import com.api.bedhcd.shared.dto.importing.ExpectedAttendanceImportRecord;
import com.api.bedhcd.shared.util.CccdUtil;
import com.api.bedhcd.util.StreamingExcelHelper;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.api.bedhcd.shared.domain.UuidFactory;

@Service
@RequiredArgsConstructor
public class ImportApplicationService {

    private final MeetingPort meetingPort;
    private final ImportJobRepository importJobRepository;
    private final AsyncImportService asyncImportService;
    private final ExpectedAttendanceRepository expectedAttendanceRepository;
    private final ReconciliationService reconciliationService;

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

    /**
     * Tạm tính (preview) khi KSNB import file danh sách tham dự dự kiến.
     * Đồng bộ - KHÔNG lưu gì vào DB, chỉ đối chiếu và trả về kết quả để người dùng xác nhận.
     */
    public ExpectedAttendancePreviewResponse previewExpectedAttendance(String meetingId, MultipartFile file) {
        if (!meetingPort.canImportShareholder(meetingId)) {
            throw ParticipantException.invalidState(
                    "Trạng thái hiện tại của cuộc họp không cho phép import danh sách tham dự.");
        }

        Map<String, ExpectedAttendanceImportRecord> records = parseAndDedupe(file);
        List<ReconciliationItemResponse> items = reconciliationService.buildItems(meetingId, records);

        long matched = items.stream().filter(i -> "KHOP".equals(i.getStatus())).count();
        long mismatched = items.stream().filter(i -> "LECH".equals(i.getStatus())).count();

        return ExpectedAttendancePreviewResponse.builder()
                .totalRows(records.size())
                .matchedCount(matched)
                .mismatchedCount(mismatched)
                .items(items)
                .build();
    }

    /**
     * Xác nhận import danh sách tham dự dự kiến (KSNB).
     * Đồng bộ, @Transactional - thay thế toàn bộ danh sách cũ của cuộc họp.
     */
    @Transactional
    public ImportExpectedResponse importExpectedAttendance(String meetingId, MultipartFile file) {
        if (!meetingPort.canImportShareholder(meetingId)) {
            throw ParticipantException.invalidState(
                    "Trạng thái hiện tại của cuộc họp không cho phép import danh sách tham dự.");
        }

        Map<String, ExpectedAttendanceImportRecord> records = parseAndDedupe(file);
        List<ReconciliationItemResponse> items = reconciliationService.buildItems(meetingId, records);

        long matched = items.stream().filter(i -> "KHOP".equals(i.getStatus())).count();
        long mismatched = items.stream().filter(i -> "LECH".equals(i.getStatus())).count();

        expectedAttendanceRepository.deleteByMeetingId(meetingId);

        List<ExpectedAttendance> toSave = records.values().stream()
                .map(r -> ExpectedAttendance.builder()
                        .meetingId(meetingId)
                        .cccd(CccdUtil.normalizeCccd(r.getCccd()))
                        .expectedShares(r.getExpectedShares() != null ? r.getExpectedShares() : 0L)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        expectedAttendanceRepository.saveAll(toSave);

        return ImportExpectedResponse.builder()
                .totalRows(records.size())
                .matchedCount(matched)
                .mismatchedCount(mismatched)
                .message("Import danh sách tham dự dự kiến thành công!")
                .build();
    }

    private Map<String, ExpectedAttendanceImportRecord> parseAndDedupe(MultipartFile file) {
        String filePath = saveTempFile(file);
        try {
            List<ExpectedAttendanceImportRecord> all = new ArrayList<>();
            try {
                StreamingExcelHelper.streamExpectedAttendance(filePath, 1000, all::addAll);
            } catch (Exception e) {
                throw ParticipantException.badRequest("Lỗi đọc file danh sách tham dự: " + e.getMessage());
            }

            Map<String, ExpectedAttendanceImportRecord> deduped = new LinkedHashMap<>();
            for (ExpectedAttendanceImportRecord record : all) {
                if (record.getCccd() != null && !record.getCccd().isBlank()) {
                    deduped.put(CccdUtil.normalizeCccd(record.getCccd()), record);
                }
            }
            return deduped;
        } finally {
            cleanupFile(filePath);
        }
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

    private void cleanupFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception e) {
            // Ignored
        }
    }
}
