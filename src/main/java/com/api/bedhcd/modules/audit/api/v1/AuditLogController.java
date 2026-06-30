package com.api.bedhcd.modules.audit.api.v1;

import com.api.bedhcd.modules.audit.api.v1.dto.AuditLogResponse;
import com.api.bedhcd.modules.audit.application.service.AuditLogApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit/logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Các API truy xuất lịch sử thao tác của Admin")
public class AuditLogController {

    private final AuditLogApplicationService auditLogApplicationService;

    @Operation(summary = "Lấy toàn bộ lịch sử (dành cho SUPER_ADMIN)")
    @GetMapping
    public ApiResponse<List<AuditLogResponse>> getAllLogs(
            @RequestParam(required = false) String adminId,
            @RequestParam(required = false) String resource) {
        // Tạm thời trả về toàn bộ log, nếu muốn lọc theo adminId/resource có thể bổ sung trong service sau.
        return ApiResponse.success(auditLogApplicationService.getAllLogs());
    }

    @Operation(summary = "Lấy lịch sử của chính Admin đang đăng nhập")
    @GetMapping("/me")
    public ApiResponse<List<AuditLogResponse>> getMyLogs() {
        String actorUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ApiResponse.success(auditLogApplicationService.getMyLogs(actorUsername));
    }
}
