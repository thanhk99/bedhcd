package com.api.bedhcd.modules.participant.api.v1;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
import com.api.bedhcd.modules.participant.application.service.ImportApplicationService;
import com.api.bedhcd.modules.participant.application.service.ImportJobApplicationService;
import com.api.bedhcd.modules.participant.api.v1.dto.ExpectedAttendancePreviewResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.ImportExpectedResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.ImportJobResponse;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/participant/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportApplicationService importService;
    private final ImportJobApplicationService importJobService;

    @PostMapping("/{meetingId}/shareholders")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_SHAREHOLDER, action = ActionCode.CREATE)
    public ApiResponse<String> importShareholders(@PathVariable String meetingId, @RequestParam("file") MultipartFile file) {
        String jobId = importService.importShareholders(meetingId, file);
        return ApiResponse.success(jobId); // Trả về jobId để client polling
    }

    @PostMapping("/{meetingId}/proxies")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_PROXY, action = ActionCode.CREATE)
    public ApiResponse<String> importProxies(@PathVariable String meetingId, @RequestParam("file") MultipartFile file) {
        String jobId = importService.importProxies(meetingId, file);
        return ApiResponse.success(jobId); // Trả về jobId để client polling
    }

    @PostMapping("/{meetingId}/expected/preview")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_SHAREHOLDER, action = ActionCode.CREATE)
    public ApiResponse<ExpectedAttendancePreviewResponse> previewExpectedAttendance(@PathVariable String meetingId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(importService.previewExpectedAttendance(meetingId, file));
    }

    @PostMapping("/{meetingId}/expected")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_SHAREHOLDER, action = ActionCode.CREATE)
    public ApiResponse<ImportExpectedResponse> importExpectedAttendance(@PathVariable String meetingId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(importService.importExpectedAttendance(meetingId, file));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<ImportJobResponse> getJobStatus(@PathVariable String jobId) {
        ImportJobResponse response = importJobService.getJobStatus(jobId);
        return ApiResponse.success(response);
    }
}
