package com.api.bedhcd.modules.voting.api.v1;

import com.api.bedhcd.modules.voting.api.v1.dto.BatchVoteRequest;
import com.api.bedhcd.modules.voting.application.service.VotingApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/voting/meetings/{meetingId}/votes")
@RequiredArgsConstructor
public class BatchVoteController {

    private final VotingApplicationService votingService;

    @PostMapping("/resolutions")
    public ApiResponse<Void> castBatchResolutions(
            @PathVariable String meetingId,
            @RequestBody BatchVoteRequest request) {
        votingService.submitBatchVotes(meetingId, request);
        return ApiResponse.success(null);
    }
}
