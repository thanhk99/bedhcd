package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.MeetingRequest;
import com.api.bedhcd.dto.response.MeetingResponse;
import com.api.bedhcd.entity.enums.MeetingStatus;
import com.api.bedhcd.service.MeetingService;
import com.api.bedhcd.service.UserService;
import com.api.bedhcd.service.VotingService;
import com.api.bedhcd.dto.UserResponse;
import com.api.bedhcd.dto.response.MeetingRealtimeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final UserService userService;
    private final VotingService votingService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeetingResponse> createMeeting(@RequestBody MeetingRequest request) {
        return ResponseEntity.ok(meetingService.createMeeting(request));
    }

    @GetMapping
    public ResponseEntity<List<MeetingResponse>> getAllMeetings() {
        return ResponseEntity.ok(meetingService.getAllMeetings());
    }

    @GetMapping("/ongoing")
    public ResponseEntity<MeetingResponse> getOngoingMeeting() {
        MeetingResponse response = meetingService.getOngoingMeeting();
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> getMeetingById(@PathVariable String id) {
        return ResponseEntity.ok(meetingService.getMeetingById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MeetingResponse> updateMeeting(@PathVariable String id, @RequestBody MeetingRequest request) {
        return ResponseEntity.ok(meetingService.updateMeeting(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMeeting(@PathVariable String id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable String id, @RequestParam MeetingStatus status) {
        meetingService.updateMeetingStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/shareholders")
    public ResponseEntity<List<UserResponse>> getShareholders(@PathVariable String id) {
        return ResponseEntity.ok(userService.getShareholdersByMeeting(id));
    }

    @GetMapping("/{id}/realtime")
    public ResponseEntity<MeetingRealtimeStatus> getRealtimeStatus(@PathVariable String id) {
        return ResponseEntity.ok(votingService.getMeetingRealtimeStatus(id));
    }
}
