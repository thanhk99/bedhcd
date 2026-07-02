package com.api.bedhcd.modules.meeting.api.v1;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingEditRequestResponse;
import com.api.bedhcd.modules.meeting.api.v1.dto.RejectEditRequestRequest;
import com.api.bedhcd.modules.meeting.application.service.MeetingApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý các yêu cầu chỉnh sửa cuộc họp (Meeting Edit Requests).
 * Chỉ SUPERADMIN hoặc admin có quyền APPROVE trên MANAGE_MEETING mới truy cập được.
 */
@RestController
@RequestMapping("/api/v1/meeting/edit-requests")
@Tag(name = "Meeting Edit Requests", description = "Quản lý yêu cầu chỉnh sửa / xóa / đổi trạng thái cuộc họp cần được duyệt")
@RequiredArgsConstructor
public class MeetingEditRequestController {

    private final MeetingApplicationService meetingApplicationService;

    @Operation(summary = "Lấy danh sách tất cả yêu cầu chỉnh sửa đang chờ duyệt (PENDING)")
    @GetMapping
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.APPROVE)
    public ApiResponse<List<MeetingEditRequestResponse>> getPendingRequests() {
        return ApiResponse.success(meetingApplicationService.getPendingRequests());
    }

    @Operation(summary = "Lấy chi tiết một yêu cầu chỉnh sửa theo ID")
    @GetMapping("/{requestId}")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.APPROVE)
    public ApiResponse<MeetingEditRequestResponse> getById(@PathVariable String requestId) {
        return ApiResponse.success(meetingApplicationService.getEditRequestById(requestId));
    }

    @Operation(summary = "Phê duyệt yêu cầu chỉnh sửa — áp dụng thay đổi vào cuộc họp")
    @PostMapping("/{requestId}/approve")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.APPROVE)
    public ApiResponse<MeetingEditRequestResponse> approve(@PathVariable String requestId) {
        return ApiResponse.success(meetingApplicationService.approveEditRequest(requestId));
    }

    @Operation(summary = "Từ chối yêu cầu chỉnh sửa — kèm lý do")
    @PostMapping("/{requestId}/reject")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.APPROVE)
    public ApiResponse<MeetingEditRequestResponse> reject(
            @PathVariable String requestId,
            @RequestBody RejectEditRequestRequest body) {
        return ApiResponse.success(meetingApplicationService.rejectEditRequest(requestId, body.getNote()));
    }
}
