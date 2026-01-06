package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.CandidateRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.request.VotingSessionRequest;
import com.api.bedhcd.dto.response.CandidateResponse;
import com.api.bedhcd.dto.response.VotingResultResponse;
import com.api.bedhcd.dto.response.VotingSessionResponse;
import com.api.bedhcd.service.VotingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VotingController {

    private final VotingService votingService;

    @PostMapping("/meetings/{meetingId}/voting-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VotingSessionResponse> createSession(@PathVariable Long meetingId,
            @RequestBody VotingSessionRequest request) {
        request.setMeetingId(meetingId);
        return ResponseEntity.ok(votingService.createVotingSession(request));
    }

    @PostMapping("/voting-sessions/{sessionId}/candidates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CandidateResponse> addCandidate(@PathVariable Long sessionId,
            @RequestBody CandidateRequest request) {
        return ResponseEntity.ok(votingService.addCandidate(sessionId, request));
    }

    @PostMapping("/voting-sessions/{sessionId}/vote")
    public ResponseEntity<Void> castVote(@PathVariable Long sessionId, @RequestBody VoteRequest request) {
        votingService.castVote(sessionId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/voting-sessions/{sessionId}/draft")
    public ResponseEntity<Void> saveDraft(@PathVariable Long sessionId, @RequestBody VoteRequest request) {
        votingService.saveDraft(sessionId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/voting-sessions/{sessionId}/results")
    public ResponseEntity<VotingResultResponse> getResults(@PathVariable Long sessionId) {
        return ResponseEntity.ok(votingService.getVotingResults(sessionId));
    }
}
