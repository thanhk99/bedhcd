package com.api.bedhcd.modules.shareholder.api.v1;

import com.api.bedhcd.modules.audit.infrastructure.security.AuditActivity;
import com.api.bedhcd.modules.shareholder.api.v1.dto.request.CreateShareholderRequest;
import com.api.bedhcd.modules.shareholder.api.v1.dto.request.UpdateShareholderRequest;
import com.api.bedhcd.modules.shareholder.api.v1.dto.response.ShareholderResponse;
import com.api.bedhcd.modules.shareholder.api.v1.dto.response.VoteHistoryResponse;
import com.api.bedhcd.modules.shareholder.application.service.ShareholderApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import com.api.bedhcd.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shareholder")
@RequiredArgsConstructor
@Tag(name = "Shareholders", description = "Các API quản lý cổ đông")
public class ShareholderController {

    private final ShareholderApplicationService shareholderService;

    @Operation(summary = "Lấy danh sách cổ đông (có phân trang và tìm kiếm)")
    @GetMapping
    public ApiResponse<PageResponse<ShareholderResponse>> getShareholders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String meetingId) {
        return ApiResponse.success(shareholderService.getShareholders(page, size, keyword, meetingId));
    }

    @Operation(summary = "Tìm kiếm nhanh top 10 cổ đông theo từ khóa")
    @GetMapping("/search")
    public ApiResponse<List<ShareholderResponse>> search(@RequestParam String keyword) {
        return ApiResponse.success(shareholderService.searchShareholders(keyword));
    }

    @Operation(summary = "Lấy lịch sử biểu quyết của cổ đông đang đăng nhập")
    @GetMapping("/me/votes")
    public ApiResponse<List<VoteHistoryResponse>> getMyVotingHistory() {
        return ApiResponse.success(shareholderService.getVotingHistory());
    }

    @Operation(summary = "Lấy chi tiết một cổ đông theo ID")
    @GetMapping("/{id}")
    public ApiResponse<ShareholderResponse> getShareholder(@PathVariable String id) {
        return ApiResponse.success(shareholderService.getShareholder(id));
    }

    @Operation(summary = "Cập nhật thông tin cổ đông")
    @PutMapping("/{id}")
    @AuditActivity(action = "UPDATE", resource = "MANAGE_SHAREHOLDER")
    public ApiResponse<ShareholderResponse> updateShareholder(
            @PathVariable String id,
            @RequestBody UpdateShareholderRequest request) {
        return ApiResponse.success(shareholderService.updateShareholder(id, request));
    }

    @Operation(summary = "Thêm mới cổ đông")
    @PostMapping
    @AuditActivity(action = "CREATE", resource = "MANAGE_SHAREHOLDER")
    public ApiResponse<ShareholderResponse> createShareholder(
            @RequestBody CreateShareholderRequest request) {
        return ApiResponse.success(shareholderService.createShareholder(request));
    }

    @Operation(summary = "Xoá mềm cổ đông")
    @DeleteMapping("/{id}")
    @AuditActivity(action = "DELETE", resource = "MANAGE_SHAREHOLDER")
    public ApiResponse<Void> deleteShareholder(@PathVariable String id) {
        shareholderService.deleteShareholder(id);
        return ApiResponse.success(null);
    }
}
