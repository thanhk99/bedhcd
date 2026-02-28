package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.AttendanceRequest;
import com.api.bedhcd.dto.response.AttendanceResponse;
import com.api.bedhcd.dto.response.CheckInBundleResponse;
import com.api.bedhcd.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attend")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/regis")
    public ResponseEntity<AttendanceResponse> registerAttendance(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.registerAttendance(request));
    }

    @GetMapping("/attended")
    public ResponseEntity<List<AttendanceResponse>> getAttendedParticipants(@RequestParam String meetingId) {
        return ResponseEntity.ok(attendanceService.getAttendedParticipants(meetingId));
    }

    @GetMapping("/checkin-bundle")
    public ResponseEntity<CheckInBundleResponse> getCheckInBundle(
            @RequestParam String meetingId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(attendanceService.getCheckInBundle(meetingId, keyword));
    }
}
