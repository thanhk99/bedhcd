package com.api.bedhcd.modules.meeting.api.v1;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingRealtimeResponse;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingResponse;
import com.api.bedhcd.modules.meeting.application.service.MeetingApplicationService;
import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingApplicationService meetingApplicationService;

    @GetMapping
    public ApiResponse<List<MeetingResponse>> getAll() {
        return ApiResponse.success(meetingApplicationService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<MeetingResponse> getById(@PathVariable String id) {
        return ApiResponse.success(meetingApplicationService.getById(id));
    }

    @GetMapping("/ongoing")
    public ApiResponse<MeetingResponse> getOngoing() {
        return ApiResponse.success(meetingApplicationService.getOngoingMeeting());
    }

    @GetMapping("/{id}/realtime")
    public ApiResponse<MeetingRealtimeResponse> getRealtime(@PathVariable String id) {
        return ApiResponse.success(meetingApplicationService.getRealtimeStats(id));
    }

    @PostMapping
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.CREATE)
    public ApiResponse<MeetingResponse> create(@RequestBody Meeting meeting) {
        return ApiResponse.success(meetingApplicationService.createMeeting(meeting));
    }

    @PutMapping("/{id}")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.UPDATE)
    public ApiResponse<MeetingResponse> update(@PathVariable String id, @RequestBody Meeting updateInfo) {
        return ApiResponse.success(meetingApplicationService.updateMeeting(id, updateInfo));
    }

    @PatchMapping("/{id}/status")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.UPDATE)
    public ApiResponse<MeetingResponse> updateStatus(@PathVariable String id, @RequestParam String status) {
        return ApiResponse.success(meetingApplicationService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @RequireAdminPermission(resource = ResourceCode.MANAGE_MEETING, action = ActionCode.DELETE)
    public ApiResponse<Void> delete(@PathVariable String id) {
        meetingApplicationService.deleteMeeting(id);
        return ApiResponse.success(null);
    }
}
