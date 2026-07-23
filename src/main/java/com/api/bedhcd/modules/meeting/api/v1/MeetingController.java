package com.api.bedhcd.modules.meeting.api.v1;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
import com.api.bedhcd.modules.audit.infrastructure.security.AuditActivity;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingRealtimeResponse;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingResponse;
import com.api.bedhcd.modules.meeting.application.service.MeetingApplicationService;
import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import com.api.bedhcd.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting")
@Tag(name = "Meeting Management", description = "Các API quản lý cuộc họp. Thao tác chỉnh sửa của ADMIN thường sẽ tạo yêu cầu chờ duyệt.")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingApplicationService meetingApplicationService;

    @Operation(summary = "Lấy danh sách cuộc họp")
    @GetMapping
    public ApiResponse<List<MeetingResponse>> getAll() {
        return ApiResponse.success(meetingApplicationService.getAll());
    }

    @Operation(summary = "Lấy thông tin chi tiết về cuộc họp theo ID")
    @GetMapping("/{id}")
    public ApiResponse<MeetingResponse> getById(@PathVariable String id) {
        return ApiResponse.success(meetingApplicationService.getById(id));
    }

    @Operation(summary = "Lấy cuộc họp đang diễn ra")
    @GetMapping("/ongoing")
    public ApiResponse<MeetingResponse> getOngoing() {
        return ApiResponse.success(meetingApplicationService.getOngoingMeetingForShareholder());
    }

    @Operation(summary = "Lấy thông tin realtime của cuộc họp theo ID")
    @GetMapping("/{id}/realtime")
    public ApiResponse<MeetingRealtimeResponse> getRealtime(@PathVariable String id) {
        return ApiResponse.success(meetingApplicationService.getRealtimeStats(id));
    }

    @Operation(summary = "Tạo cuộc họp mới")
    @PostMapping
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.CREATE)
    @AuditActivity(action = "CREATE", resource = "MANAGE_MEETING")
    public ApiResponse<MeetingResponse> create(@RequestBody Meeting meeting) {
        return ApiResponse.success(meetingApplicationService.createMeeting(meeting));
    }

    /**
     * Cập nhật cuộc họp.
     * - SUPERADMIN: Cập nhật trực tiếp, trả về MeetingResponse.
     * - ADMIN thường: Tạo yêu cầu chờ duyệt, trả về MeetingEditRequestResponse
     * (requiresApproval=true).
     */
    @Operation(summary = "Cập nhật cuộc họp. SUPERADMIN cập nhật trực tiếp; ADMIN thường tạo yêu cầu chờ duyệt.")
    @PutMapping("/{id}")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.UPDATE)
    @AuditActivity(action = "UPDATE", resource = "MANAGE_MEETING")
    public ApiResponse<Object> update(@PathVariable String id, @RequestBody Meeting updateInfo) {
        return ApiResponse.success(meetingApplicationService.updateMeeting(id, updateInfo));
    }

    /**
     * Cập nhật trạng thái cuộc họp.
     * - SUPERADMIN: Cập nhật trực tiếp.
     * - ADMIN thường: Tạo yêu cầu chờ duyệt.
     */
    @Operation(summary = "Cập nhật trạng thái cuộc họp. SUPERADMIN cập nhật trực tiếp; ADMIN thường tạo yêu cầu chờ duyệt.")
    @PatchMapping("/{id}/status")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.UPDATE)
    @AuditActivity(action = "UPDATE", resource = "MANAGE_MEETING")
    public ApiResponse<Object> updateStatus(@PathVariable String id, @RequestParam String status) {
        return ApiResponse.success(meetingApplicationService.updateStatus(id, status));
    }

    /**
     * Xóa cuộc họp.
     * - SUPERADMIN: Xóa trực tiếp (trả về null data).
     * - ADMIN thường: Tạo yêu cầu xóa chờ duyệt, trả về MeetingEditRequestResponse.
     */
    @Operation(summary = "Xóa cuộc họp. SUPERADMIN xóa trực tiếp; ADMIN thường tạo yêu cầu xóa chờ duyệt.")
    @DeleteMapping("/{id}")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.DELETE)
    @AuditActivity(action = "DELETE", resource = "MANAGE_MEETING")
    public ApiResponse<Object> delete(@PathVariable String id) {
        return ApiResponse.success(meetingApplicationService.deleteMeeting(id));
    }
}
