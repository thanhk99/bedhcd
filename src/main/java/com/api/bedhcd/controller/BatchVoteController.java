package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.BatchVoteRequest;
import com.api.bedhcd.entity.enums.ElectionType;
import com.api.bedhcd.service.ElectionService;
import com.api.bedhcd.service.VotingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meetings/{meetingId}/votes")
@RequiredArgsConstructor
public class BatchVoteController {

    private final VotingService votingService;
    private final ElectionService electionService;

    @PostMapping("/resolutions")
    public ResponseEntity<Void> castBatchResolutions(
            @PathVariable String meetingId,
            @RequestBody BatchVoteRequest request,
            HttpServletRequest servletRequest) {
        votingService.castBatchResolutions(meetingId, request, servletRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/elections/bod")
    public ResponseEntity<Void> castBatchBoardOfDirectors(
            @PathVariable String meetingId,
            @RequestBody BatchVoteRequest request,
            HttpServletRequest servletRequest) {
        electionService.castBatchElections(meetingId, request, ElectionType.BOARD_OF_DIRECTORS, servletRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/elections/sb")
    public ResponseEntity<Void> castBatchSupervisoryBoard(
            @PathVariable String meetingId,
            @RequestBody BatchVoteRequest request,
            HttpServletRequest servletRequest) {
        electionService.castBatchElections(meetingId, request, ElectionType.SUPERVISORY_BOARD, servletRequest);
        return ResponseEntity.ok().build();
    }
}
