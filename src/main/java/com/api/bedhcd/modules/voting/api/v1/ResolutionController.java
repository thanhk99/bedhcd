package com.api.bedhcd.modules.voting.api.v1;

import com.api.bedhcd.modules.voting.api.v1.dto.ResolutionRequest;
import com.api.bedhcd.modules.voting.api.v1.dto.ResolutionResponse;
import com.api.bedhcd.modules.voting.api.v1.dto.VoteRequest;
import com.api.bedhcd.modules.voting.api.v1.dto.VotingResultResponse;
import com.api.bedhcd.modules.voting.application.service.VotingApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResolutionController {

    private final VotingApplicationService votingService;

    @GetMapping("/api/meetings/{meetingId}/resolutions")
    public ApiResponse<List<ResolutionResponse>> getByMeeting(@PathVariable String meetingId) {
        return ApiResponse.success(votingService.getByMeeting(meetingId));
    }

    @PostMapping("/api/meetings/{meetingId}/resolutions")
    public ApiResponse<ResolutionResponse> create(@PathVariable String meetingId, @RequestBody ResolutionRequest request) {
        return ApiResponse.success(votingService.createResolution(meetingId, request));
    }

    @GetMapping("/api/resolutions/{id}")
    public ApiResponse<ResolutionResponse> getById(@PathVariable String id) {
        return ApiResponse.success(votingService.getById(id));
    }

    @PostMapping("/api/resolutions/{id}/vote")
    public ApiResponse<Void> vote(@PathVariable String id, @RequestBody VoteRequest request) {
        votingService.submitVote(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/api/resolutions/{id}/results")
    public ApiResponse<VotingResultResponse> getResults(@PathVariable String id) {
        return ApiResponse.success(votingService.getResults(id));
    }
}
