package com.api.bedhcd.modules.participant.api.v1;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
import com.api.bedhcd.modules.participant.application.service.ImportApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/participant/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportApplicationService importService;

    @PostMapping("/{meetingId}/shareholders")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_SHAREHOLDER, action = ActionCode.CREATE)
    public ApiResponse<String> importShareholders(@PathVariable String meetingId, @RequestParam("file") MultipartFile file) {
        importService.importShareholders(meetingId, file);
        return ApiResponse.success("Đã nhập danh sách cổ đông thành công");
    }

    @PostMapping("/{meetingId}/proxies")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_PROXY, action = ActionCode.CREATE)
    public ApiResponse<String> importProxies(@PathVariable String meetingId, @RequestParam("file") MultipartFile file) {
        importService.importProxies(meetingId, file);
        return ApiResponse.success("Đã nhập danh sách ủy quyền thành công");
    }
}
