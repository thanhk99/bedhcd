package com.api.bedhcd.modules.resolution.api.v1;

import com.api.bedhcd.modules.resolution.application.service.ResolutionApplicationService;
import com.api.bedhcd.modules.voting.api.v1.dto.ResolutionRequest;
import com.api.bedhcd.modules.voting.api.v1.dto.ResolutionResponse;
import com.api.bedhcd.modules.voting.api.v1.dto.VoteRequest;
import com.api.bedhcd.modules.voting.api.v1.dto.VotingResultResponse;
import com.api.bedhcd.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.api.bedhcd.modules.meeting.application.service.MeetingApplicationService;
import com.api.bedhcd.modules.meeting.api.v1.dto.BatchApprovalRequest;

@RestController
@RequestMapping("/api/v1/resolution")
@Tag(name = "Resolution Management", description = "Quản lý các chức năng liên quan đến nghị quyết, bao gồm tạo, cập nhật, xóa và bỏ phiếu")
@RequiredArgsConstructor
public class ResolutionController {

    private final ResolutionApplicationService resolutionService;
    private final MeetingApplicationService meetingApplicationService;

    @Operation(summary = "Lấy danh sách nghị quyết của cuộc họp theo ID")
    @GetMapping("meeting/{meetingId}")
    public ApiResponse<List<ResolutionResponse>> listResolutionsByMeetingId(@PathVariable String meetingId) {
        return ApiResponse.success(resolutionService.listResolutionsByMeetingId(meetingId));
    }

    @Operation(summary = "Tạo nghị quyết mới cho cuộc họp (Chờ duyệt)")
    @PostMapping("meeting/{meetingId}")
    public ApiResponse<Object> create(@PathVariable String meetingId,
            @RequestBody ResolutionRequest request) {
        BatchApprovalRequest batch = new BatchApprovalRequest();
        batch.setResolutions(List.of(new BatchApprovalRequest.ResolutionOperation("CREATE", null, request, null)));
        batch.setNote("Tạo mới nghị quyết");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }

    @Operation(summary = "Cập nhật nghị quyết (Chờ duyệt)")
    @PutMapping("/{meetingId}/{id}")
    public ApiResponse<Object> updateResolution(@PathVariable String meetingId, @PathVariable String id,
            @RequestBody ResolutionRequest request) {
        BatchApprovalRequest batch = new BatchApprovalRequest();
        batch.setResolutions(List.of(new BatchApprovalRequest.ResolutionOperation("UPDATE", id, request, null)));
        batch.setNote("Cập nhật nghị quyết");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }

    @Operation(summary = "Xóa nghị quyết (Chờ duyệt)")
    @DeleteMapping("/{meetingId}/{id}")
    public ApiResponse<Object> deleteResolution(@PathVariable String meetingId, @PathVariable String id) {
        BatchApprovalRequest batch = new BatchApprovalRequest();
        batch.setResolutions(List.of(new BatchApprovalRequest.ResolutionOperation("DELETE", id, null, null)));
        batch.setNote("Xóa nghị quyết");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }

    @Operation(summary = "Lấy thông tin chi tiết về nghị quyết")
    @GetMapping("/{id}")
    public ApiResponse<ResolutionResponse> getById(@PathVariable String id) {
        return ApiResponse.success(resolutionService.getById(id));
    }

    @Operation(summary = "Bỏ phiếu cho nghị quyết")
    @PostMapping("/{id}/vote")
    public ApiResponse<Void> vote(@PathVariable String id, @RequestBody VoteRequest request) {
        resolutionService.submitVote(id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Lấy kết quả bỏ phiếu cho nghị quyết")
    @GetMapping("/{id}/results")
    public ApiResponse<VotingResultResponse> getResults(@PathVariable String id) {
        return ApiResponse.success(resolutionService.getResults(id));
    }
}
