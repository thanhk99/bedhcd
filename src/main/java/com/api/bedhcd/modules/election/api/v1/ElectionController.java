package com.api.bedhcd.modules.election.api.v1;

import com.api.bedhcd.modules.election.api.v1.dto.CandidateRequest;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionRequest;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResponse;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResultResponse;
import com.api.bedhcd.modules.election.application.service.ElectionApplicationService;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionVoteRequest;
import com.api.bedhcd.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/election/")
@Tag(name = "Elections", description = "Các api Quản lý bầu cử ")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionApplicationService electionService;

    @GetMapping("{meetingId}/elections")
    @Operation(summary = "Lấy danh sách bầu cử theo cuộc họp", description = "Lấy danh sách bầu cử theo cuộc họp")
    public ApiResponse<List<ElectionResponse>> getByMeeting(@PathVariable String meetingId) {
        return ApiResponse.success(electionService.getByMeeting(meetingId));
    }

    @PostMapping("{meetingId}/elections")
    @Operation(summary = "Tạo danh sách bầu cử theo cuộc họp", description = "Tạo danh sách bầu cử theo cuộc họp")
    public ApiResponse<ElectionResponse> create(@PathVariable String meetingId, @RequestBody ElectionRequest request) {
        return ApiResponse.success(electionService.createElection(meetingId, request));
    }

    @PutMapping("elections/{id}")
    @Operation(summary = "Cập nhật danh sách bầu cử", description = "Cập nhật danh sách bầu cử")
    public ApiResponse<ElectionResponse> update(@PathVariable String id, @RequestBody ElectionRequest request) {
        return ApiResponse.success(electionService.updateElection(id, request));
    }

    @PostMapping("{id}/candidates")
    @Operation(summary = "Thêm ứng viên", description = "Thêm ứng viên")
    public ApiResponse<ElectionResponse> addCandidate(@PathVariable String id,
            @RequestBody CandidateRequest request) {
        return ApiResponse.success(electionService.addCandidate(id, request));
    }

    @PutMapping("{id}/candidates/{candidateId}")
    @Operation(summary = "Cập nhật ứng viên", description = "Cập nhật ứng viên")
    public ApiResponse<ElectionResponse> updateCandidate(@PathVariable String id, @PathVariable String candidateId,
            @RequestBody CandidateRequest request) {
        return ApiResponse.success(electionService.updateCandidate(id, candidateId, request));
    }

    @PostMapping("{id}/vote")
    @Operation(summary = "Bỏ phiếu", description = "Bỏ phiếu")
    public ApiResponse<Void> vote(@PathVariable String id, @RequestBody ElectionVoteRequest request) {
        electionService.submitVote(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping("{id}/results")
    @Operation(summary = "Lấy kết quả bầu cử", description = "Lấy kết quả bầu cử")
    public ApiResponse<ElectionResultResponse> getResults(@PathVariable String id) {
        return ApiResponse.success(electionService.getResults(id));
    }

    @DeleteMapping("{meetingId}/elections/{id}")
    @Operation(summary = "Xóa cuộc bầu cử", description = "Xóa cuộc bầu cử")
    public ApiResponse<Void> deleteElection(@PathVariable String meetingId, @PathVariable String id) {
        electionService.deleteElection(meetingId, id);
        return ApiResponse.success(null);
    }
}
