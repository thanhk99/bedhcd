package com.api.bedhcd.modules.meeting.api.v1;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
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
@RequestMapping("/api/v1/meeting/meetings")
@Tag(name = "Meeting Management", description = "Các API dành cho SUPER_ADMIN để quản lý tài khoản và quyền của ADMIN")
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
        return ApiResponse.success(meetingApplicationService.getOngoingMeeting());
    }

    @Operation(summary = "Lấy thông tin realtime của cuộc họp theo ID")
    @GetMapping("/{id}/realtime")
    public ApiResponse<MeetingRealtimeResponse> getRealtime(@PathVariable String id) {
        return ApiResponse.success(meetingApplicationService.getRealtimeStats(id));
    }

    @Operation(summary = "Tạo cuộc họp mới")
    @PostMapping
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.CREATE)
    public ApiResponse<MeetingResponse> create(@RequestBody Meeting meeting) {
        return ApiResponse.success(meetingApplicationService.createMeeting(meeting));
    }

    @Operation(summary = "Cập nhật cuộc họp")
    @PutMapping("/{id}")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.UPDATE)
    public ApiResponse<MeetingResponse> update(@PathVariable String id, @RequestBody Meeting updateInfo) {
        return ApiResponse.success(meetingApplicationService.updateMeeting(id, updateInfo));
    }

    @Operation(summary = "Cập nhật trạng thái cuộc họp")
    @PatchMapping("/{id}/status")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.UPDATE)
    public ApiResponse<MeetingResponse> updateStatus(@PathVariable String id, @RequestParam String status) {
        return ApiResponse.success(meetingApplicationService.updateStatus(id, status));
    }

    @Operation(summary = "Xóa cuộc họp")
    @DeleteMapping("/{id}")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.DELETE)
    public ApiResponse<Void> delete(@PathVariable String id) {
        meetingApplicationService.deleteMeeting(id);
        return ApiResponse.success(null);
    }

}
