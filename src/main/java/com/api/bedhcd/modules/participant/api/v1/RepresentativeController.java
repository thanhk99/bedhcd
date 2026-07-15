package com.api.bedhcd.modules.participant.api.v1;

import com.api.bedhcd.modules.participant.application.service.ProxyApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý tạo người nhận uỷ quyền không phải cổ đông (đại diện).
 * Frontend gọi: POST /api/representatives
 */
@RestController
@RequestMapping("/api/v1/representatives")
@RequiredArgsConstructor
public class RepresentativeController {

    private final ProxyApplicationService proxyService;

    @PostMapping
    public ApiResponse<Map<String, Object>> createRepresentative(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(proxyService.createRepresentative(request));
    }
}
