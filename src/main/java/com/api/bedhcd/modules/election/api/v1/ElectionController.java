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

import com.api.bedhcd.modules.meeting.application.service.MeetingApplicationService;
import com.api.bedhcd.modules.meeting.api.v1.dto.BatchApprovalRequest;

@RestController
@RequestMapping("/api/v1/election/")
@Tag(name = "Elections", description = "Các api Quản lý bầu cử ")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionApplicationService electionService;
    private final com.api.bedhcd.modules.election.domain.repository.ElectionRepository electionRepository;
    private final MeetingApplicationService meetingApplicationService;

    @GetMapping("{meetingId}/elections")
    @Operation(summary = "Lấy danh sách bầu cử theo cuộc họp", description = "Lấy danh sách bầu cử theo cuộc họp")
    public ApiResponse<List<ElectionResponse>> getByMeeting(@PathVariable String meetingId) {
        return ApiResponse.success(electionService.getByMeeting(meetingId));
    }

    @PostMapping("{meetingId}/elections")
    @Operation(summary = "Tạo danh sách bầu cử theo cuộc họp (Chờ duyệt)", description = "Tạo danh sách bầu cử theo cuộc họp")
    public ApiResponse<Object> create(@PathVariable String meetingId, @RequestBody ElectionRequest request) {
        BatchApprovalRequest batch = new BatchApprovalRequest();
        BatchApprovalRequest.ElectionData data = new BatchApprovalRequest.ElectionData(
            request.getTitle(), request.getDescription(), request.getType(), request.getDisplayOrder(), null);
        batch.setElections(List.of(new BatchApprovalRequest.ElectionOperation("CREATE", null, data, null)));
        batch.setNote("Tạo mới bầu cử");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }

    @PutMapping("elections/{id}")
    @Operation(summary = "Cập nhật danh sách bầu cử (Chờ duyệt)", description = "Cập nhật danh sách bầu cử")
    public ApiResponse<Object> update(@PathVariable String id, @RequestBody ElectionRequest request) {
        String meetingId = electionRepository.findById(id)
            .orElseThrow(() -> com.api.bedhcd.modules.election.domain.exception.ElectionException.electionNotFound(id))
            .getMeetingId();
        BatchApprovalRequest batch = new BatchApprovalRequest();
        BatchApprovalRequest.ElectionData data = new BatchApprovalRequest.ElectionData(
            request.getTitle(), request.getDescription(), request.getType(), request.getDisplayOrder(), null);
        batch.setElections(List.of(new BatchApprovalRequest.ElectionOperation("UPDATE", id, data, null)));
        batch.setNote("Cập nhật bầu cử");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }

    @PostMapping("{id}/candidates")
    @Operation(summary = "Thêm ứng viên (Chờ duyệt)", description = "Thêm ứng viên")
    public ApiResponse<Object> addCandidate(@PathVariable String id,
            @RequestBody CandidateRequest request) {
        String meetingId = electionRepository.findById(id)
            .orElseThrow(() -> com.api.bedhcd.modules.election.domain.exception.ElectionException.electionNotFound(id))
            .getMeetingId();
        BatchApprovalRequest batch = new BatchApprovalRequest();
        BatchApprovalRequest.ElectionData data = new BatchApprovalRequest.ElectionData();
        data.setCandidates(List.of(new BatchApprovalRequest.CandidateOperation("CREATE", null, request, null)));
        batch.setElections(List.of(new BatchApprovalRequest.ElectionOperation("UPDATE", id, data, null)));
        batch.setNote("Thêm ứng viên");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }

    @PutMapping("{id}/candidates/{candidateId}")
    @Operation(summary = "Cập nhật ứng viên (Chờ duyệt)", description = "Cập nhật ứng viên")
    public ApiResponse<Object> updateCandidate(@PathVariable String id, @PathVariable String candidateId,
            @RequestBody CandidateRequest request) {
        String meetingId = electionRepository.findById(id)
            .orElseThrow(() -> com.api.bedhcd.modules.election.domain.exception.ElectionException.electionNotFound(id))
            .getMeetingId();
        BatchApprovalRequest batch = new BatchApprovalRequest();
        BatchApprovalRequest.ElectionData data = new BatchApprovalRequest.ElectionData();
        data.setCandidates(List.of(new BatchApprovalRequest.CandidateOperation("UPDATE", candidateId, request, null)));
        batch.setElections(List.of(new BatchApprovalRequest.ElectionOperation("UPDATE", id, data, null)));
        batch.setNote("Cập nhật ứng viên");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
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
    @Operation(summary = "Xóa cuộc bầu cử (Chờ duyệt)", description = "Xóa cuộc bầu cử")
    public ApiResponse<Object> deleteElection(@PathVariable String meetingId, @PathVariable String id) {
        BatchApprovalRequest batch = new BatchApprovalRequest();
        batch.setElections(List.of(new BatchApprovalRequest.ElectionOperation("DELETE", id, null, null)));
        batch.setNote("Xóa bầu cử");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }

    @DeleteMapping("{electionId}/candidates/{candidateId}")
    @Operation(summary = "Xóa ứng viên (Chờ duyệt)", description = "Xóa một ứng viên khỏi cuộc bầu cử")
    public ApiResponse<Object> deleteCandidate(@PathVariable String electionId, @PathVariable String candidateId) {
        String meetingId = electionRepository.findById(electionId)
            .orElseThrow(() -> com.api.bedhcd.modules.election.domain.exception.ElectionException.electionNotFound(electionId))
            .getMeetingId();
        BatchApprovalRequest batch = new BatchApprovalRequest();
        BatchApprovalRequest.ElectionData data = new BatchApprovalRequest.ElectionData();
        data.setCandidates(List.of(new BatchApprovalRequest.CandidateOperation("DELETE", candidateId, null, null)));
        batch.setElections(List.of(new BatchApprovalRequest.ElectionOperation("UPDATE", electionId, data, null)));
        batch.setNote("Xóa ứng viên");
        return ApiResponse.success(meetingApplicationService.submitBatchApproval(meetingId, batch));
    }
}
