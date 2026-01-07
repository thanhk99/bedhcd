package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.CandidateRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.response.CandidateResponse;
import com.api.bedhcd.dto.response.VotingResultResponse;
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

    @PostMapping("/meetings/{meetingId}/voting-items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.api.bedhcd.entity.VotingItem> createVotingItem(@PathVariable Long meetingId,
            @RequestBody com.api.bedhcd.dto.request.VotingItemRequest request) {
        return ResponseEntity.ok(votingService.createVotingItem(meetingId, request));
    }

    @PostMapping("/voting-items/{votingItemId}/candidates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CandidateResponse> addCandidate(@PathVariable Long votingItemId,
            @RequestBody CandidateRequest request) {
        return ResponseEntity.ok(votingService.addCandidate(votingItemId, request));
    }

    @PostMapping("/voting-items/{votingItemId}/vote")
    public ResponseEntity<Void> castVote(@PathVariable Long votingItemId, @RequestBody VoteRequest request) {
        votingService.castVote(votingItemId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/voting-items/{votingItemId}/draft")
    public ResponseEntity<Void> saveDraft(@PathVariable Long votingItemId, @RequestBody VoteRequest request) {
        votingService.saveDraft(votingItemId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/voting-items/{votingItemId}/results")
    public ResponseEntity<VotingResultResponse> getResults(@PathVariable Long votingItemId) {
        return ResponseEntity.ok(votingService.getVotingResults(votingItemId));
    }
}
