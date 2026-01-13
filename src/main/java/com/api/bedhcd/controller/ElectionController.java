package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.VotingOptionRequest;
import com.api.bedhcd.dto.request.ElectionRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.response.VotingOptionResponse;
import com.api.bedhcd.dto.response.ElectionResponse;
import com.api.bedhcd.dto.response.VotingResultResponse;
import com.api.bedhcd.service.ElectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionService electionService;

    // --- Quản lý Bầu cử ---

    @PostMapping("/meetings/{meetingId}/elections")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ElectionResponse> createElection(
            @PathVariable String meetingId,
            @RequestBody ElectionRequest request) {
        return ResponseEntity.ok(electionService.createElection(meetingId, request));
    }

    @GetMapping("/elections/{electionId}")
    public ResponseEntity<ElectionResponse> getElection(@PathVariable String electionId) {
        return ResponseEntity.ok(electionService.getElectionById(electionId));
    }

    @PostMapping("/elections/{electionId}/options")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VotingOptionResponse> addVotingOption(
            @PathVariable String electionId,
            @RequestBody VotingOptionRequest request) {
        return ResponseEntity.ok(electionService.addVotingOption(electionId, request));
    }

    // --- Bỏ phiếu bầu cử ---

    @PostMapping("/elections/{electionId}/vote")
    public ResponseEntity<Void> castVote(
            @PathVariable String electionId,
            @RequestBody VoteRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        electionService.castVote(electionId, request, servletRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/elections/{electionId}/draft")
    public ResponseEntity<Void> saveDraft(
            @PathVariable String electionId,
            @RequestBody VoteRequest request) {
        electionService.saveDraft(electionId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/elections/{electionId}/results")
    public ResponseEntity<VotingResultResponse> getResults(@PathVariable String electionId) {
        return ResponseEntity.ok(electionService.getVotingResults(electionId));
    }

    @PostMapping("/elections/{electionId}/edit")
    public ResponseEntity<?> editElection(@PathVariable String electionId, @RequestBody ElectionRequest request) {
        return ResponseEntity.ok(electionService.editElection(electionId, request));
    }
}
