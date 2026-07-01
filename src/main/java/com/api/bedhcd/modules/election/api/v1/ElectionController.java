package com.api.bedhcd.modules.election.api.v1;

import com.api.bedhcd.modules.election.api.v1.dto.ElectionRequest;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResponse;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResultResponse;
import com.api.bedhcd.modules.election.application.service.ElectionApplicationService;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionVoteRequest;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/election/")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionApplicationService electionService;

    @GetMapping("{meetingId}/elections")
    public ApiResponse<List<ElectionResponse>> getByMeeting(@PathVariable String meetingId) {
        return ApiResponse.success(electionService.getByMeeting(meetingId));
    }

    @PostMapping("{meetingId}/elections")
    public ApiResponse<ElectionResponse> create(@PathVariable String meetingId, @RequestBody ElectionRequest request) {
        return ApiResponse.success(electionService.createElection(meetingId, request));
    }

    @PostMapping("{id}/candidates")
    public ApiResponse<ElectionResponse> addCandidate(@PathVariable String id,
            @RequestBody com.api.bedhcd.modules.election.api.v1.dto.CandidateRequest request) {
        return ApiResponse.success(electionService.addCandidate(id, request));
    }

    @PostMapping("{id}/vote")
    public ApiResponse<Void> vote(@PathVariable String id, @RequestBody ElectionVoteRequest request) {
        electionService.submitVote(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping("{id}/results")
    public ApiResponse<ElectionResultResponse> getResults(@PathVariable String id) {
        return ApiResponse.success(electionService.getResults(id));
    }
}
