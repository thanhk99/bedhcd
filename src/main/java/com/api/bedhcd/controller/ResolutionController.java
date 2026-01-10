package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.ResolutionRequest;
import com.api.bedhcd.dto.request.VotingOptionRequest;
import com.api.bedhcd.dto.request.VoteRequest;
import com.api.bedhcd.dto.response.ResolutionResponse;
import com.api.bedhcd.dto.response.VotingOptionResponse;
import com.api.bedhcd.dto.response.VotingResultResponse;
import com.api.bedhcd.service.VotingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResolutionController {

    private final VotingService votingService;

    // --- Quản lý Nghị quyết ---

    @PostMapping("/meetings/{meetingId}/resolutions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResolutionResponse> createResolution(
            @PathVariable String meetingId,
            @RequestBody ResolutionRequest request) {
        return ResponseEntity.ok(votingService.createResolution(meetingId, request));
    }

    @GetMapping("/meetings/{meetingId}/resolutions")
    public ResponseEntity<List<ResolutionResponse>> getResolutionsByMeeting(
            @PathVariable String meetingId) {
        return ResponseEntity.ok(votingService.getResolutionsByMeetingId(meetingId));
    }

    @GetMapping("/resolutions/{resolutionId}")
    public ResponseEntity<ResolutionResponse> getResolution(@PathVariable String resolutionId) {
        return ResponseEntity.ok(votingService.getResolutionById(resolutionId));
    }

    @PutMapping("/resolutions/{resolutionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResolutionResponse> updateResolution(
            @PathVariable String resolutionId,
            @RequestBody ResolutionRequest request) {
        return ResponseEntity.ok(votingService.updateResolution(resolutionId, request));
    }

    @DeleteMapping("/resolutions/{resolutionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteResolution(@PathVariable String resolutionId) {
        votingService.deleteResolution(resolutionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/options/{optionId}")
    public ResponseEntity<VotingOptionResponse> getVotingOption(@PathVariable String optionId) {
        return ResponseEntity.ok(votingService.getVotingOptionById(optionId));
    }

    @PutMapping("/options/{optionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VotingOptionResponse> updateVotingOption(
            @PathVariable String optionId,
            @RequestBody VotingOptionRequest request) {
        return ResponseEntity.ok(votingService.updateVotingOption(optionId, request));
    }

    @DeleteMapping("/options/{optionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVotingOption(@PathVariable String optionId) {
        votingService.deleteVotingOption(optionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resolutions/{resolutionId}/options")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VotingOptionResponse> addVotingOption(
            @PathVariable String resolutionId,
            @RequestBody VotingOptionRequest request) {
        return ResponseEntity.ok(votingService.addVotingOptionToResolution(resolutionId, request));
    }

    // --- Biểu quyết nghị quyết ---

    @PostMapping("/resolutions/{resolutionId}/vote")
    public ResponseEntity<Void> castVote(
            @PathVariable String resolutionId,
            @RequestBody VoteRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        votingService.castVote(resolutionId, request, servletRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resolutions/{resolutionId}/draft")
    public ResponseEntity<Void> saveDraft(
            @PathVariable String resolutionId,
            @RequestBody VoteRequest request) {
        votingService.saveDraft(resolutionId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/resolutions/{resolutionId}/results")
    public ResponseEntity<VotingResultResponse> getResults(@PathVariable String resolutionId) {
        return ResponseEntity.ok(votingService.getVotingResults(resolutionId));
    }
}
