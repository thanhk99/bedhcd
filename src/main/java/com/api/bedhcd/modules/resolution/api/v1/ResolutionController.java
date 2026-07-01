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

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Resolution Management", description = "Quản lý các chức năng liên quan đến nghị quyết, bao gồm tạo, cập nhật, xóa và bỏ phiếu")
@RequiredArgsConstructor
public class ResolutionController {

    private final ResolutionApplicationService resolutionService;

    @Operation(summary = "Lấy danh sách nghị quyết của cuộc họp theo ID")
    @GetMapping("/meeting/meetings/{meetingId}/resolutions")
    public ApiResponse<List<ResolutionResponse>> listResolutionsByMeetingId(@PathVariable String meetingId) {
        return ApiResponse.success(resolutionService.listResolutionsByMeetingId(meetingId));
    }

    @Operation(summary = "Tạo nghị quyết mới cho cuộc họp")
    @PostMapping("/meeting/meetings/{meetingId}/resolutions")
    public ApiResponse<ResolutionResponse> create(@PathVariable String meetingId,
            @RequestBody ResolutionRequest request) {
        return ApiResponse.success(resolutionService.createResolution(meetingId, request));
    }

    @Operation(summary = "Cập nhật nghị quyết")
    @PatchMapping("/meeting/meetings/{meetingId}/resolutions/{id}")
    public ApiResponse<ResolutionResponse> updateResolution(@PathVariable String meetingId, @PathVariable String id,
            @RequestBody ResolutionRequest request) {
        return ApiResponse.success(resolutionService.updateResolution(meetingId, id, request));
    }

    @Operation(summary = "Xóa nghị quyết")
    @DeleteMapping("/meeting/meetings/{meetingId}/resolutions/{id}")
    public ApiResponse<Void> deleteResolution(@PathVariable String meetingId, @PathVariable String id) {
        resolutionService.deleteResolution(meetingId, id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Lấy thông tin chi tiết về nghị quyết")
    @GetMapping("/resolutions/{id}")
    public ApiResponse<ResolutionResponse> getById(@PathVariable String id) {
        return ApiResponse.success(resolutionService.getById(id));
    }

    @Operation(summary = "Bỏ phiếu cho nghị quyết")
    @PostMapping("/resolutions/{id}/vote")
    public ApiResponse<Void> vote(@PathVariable String id, @RequestBody VoteRequest request) {
        resolutionService.submitVote(id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Lấy kết quả bỏ phiếu cho nghị quyết")
    @GetMapping("/resolutions/{id}/results")
    public ApiResponse<VotingResultResponse> getResults(@PathVariable String id) {
        return ApiResponse.success(resolutionService.getResults(id));
    }
}
