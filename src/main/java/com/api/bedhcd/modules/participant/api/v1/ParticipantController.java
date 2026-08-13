package com.api.bedhcd.modules.participant.api.v1;

import com.api.bedhcd.modules.participant.api.v1.dto.AttendanceRequest;
import com.api.bedhcd.modules.participant.api.v1.dto.AttendanceResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.CheckInBundleResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.ReconciliationResponse;
import com.api.bedhcd.modules.participant.application.service.ParticipantApplicationService;
import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import com.api.bedhcd.modules.admin.infrastructure.security.RequireAdminPermission;
import com.api.bedhcd.shared.dto.ApiResponse;
import com.api.bedhcd.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/participant")
@Tag(name = "Người tham dự cuộc họp", description = "API dành cho Người tham dự")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantApplicationService participantService;

    @Operation(summary = "Đăng ký tham dự cuộc họp")
    @PostMapping("/regis")
    public ApiResponse<AttendanceResponse> register(@RequestBody AttendanceRequest request) {
        return ApiResponse.success(participantService.registerAttendance(request));
    }

    @Operation(summary = "Cập nhật tham dự cuộc họp")
    @PostMapping("/update")
    public ApiResponse<AttendanceResponse> update(@RequestBody AttendanceRequest request) {
        return ApiResponse.success(participantService.updateAttendance(request));
    }

    @Operation(summary = "Hủy đăng ký tham dự cuộc họp")
    @PostMapping("/cancel")
    public ApiResponse<AttendanceResponse> cancel(@RequestParam String meetingId, @RequestParam String cccd) {
        return ApiResponse.success(participantService.cancelAttendance(meetingId, cccd));
    }

    @Operation(summary = "Lấy danh sách đã điểm danh (có phân trang và tìm kiếm)")
    @GetMapping("/list/{meetingId}")
    public ApiResponse<PageResponse<AttendanceResponse>> getList(
            @PathVariable String meetingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(participantService.getAttendedParticipants(meetingId, page, size, keyword));
    }

    @Operation(summary = "Lấy thông tin")
    @GetMapping("/bundle")
    public ApiResponse<CheckInBundleResponse> getBundle(@RequestParam("meetingId") String meetingId,
            @RequestParam("cccd") String cccd) {
        return ApiResponse.success(participantService.getCheckInBundle(meetingId, cccd));
    }

    @Operation(summary = "Tìm kiếm nhanh người tham dự gồm cả chưa điểm danh (dùng cho autocomplete)")
    @GetMapping("/search")
    public ApiResponse<List<AttendanceResponse>> searchParticipants(
            @RequestParam("meetingId") String meetingId,
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(participantService.searchParticipants(meetingId, keyword, size));
    }

    @Operation(summary = "Đánh dấu đã in")
    @PostMapping("/print")
    public ApiResponse<AttendanceResponse> markAsPrinted(@RequestParam String meetingId, @RequestParam String cccd) {
        return ApiResponse.success(participantService.markAsPrinted(meetingId, cccd));
    }

    @Operation(summary = "Đối soát số lượng cổ phần tham dự (tạm thời vs thực tế)")
    @GetMapping("/reconcile/{meetingId}")
    @RequireAdminPermission(resource = ResourceCode.RECONCILE, action = ActionCode.VIEW)
    public ApiResponse<ReconciliationResponse> getReconciliation(@PathVariable String meetingId) {
        return ApiResponse.success(participantService.getReconciliation(meetingId));
    }
}
