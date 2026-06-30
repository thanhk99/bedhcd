package com.api.bedhcd.modules.participant.api.v1;

import com.api.bedhcd.modules.participant.api.v1.dto.AttendanceRequest;
import com.api.bedhcd.modules.participant.api.v1.dto.AttendanceResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.CheckInBundleResponse;
import com.api.bedhcd.modules.participant.application.service.ParticipantApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/participant/attendances")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantApplicationService participantService;

    @PostMapping("/regis")
    public ApiResponse<AttendanceResponse> register(@RequestBody AttendanceRequest request) {
        return ApiResponse.success(participantService.registerAttendance(request));
    }

    @PostMapping("/cancel")
    public ApiResponse<AttendanceResponse> cancel(@RequestParam String meetingId, @RequestParam String cccd) {
        return ApiResponse.success(participantService.cancelAttendance(meetingId, cccd));
    }

    @GetMapping("/list/{meetingId}")
    public ApiResponse<List<AttendanceResponse>> getList(@PathVariable String meetingId) {
        return ApiResponse.success(participantService.getAttendedParticipants(meetingId));
    }

    @GetMapping("/bundle")
    public ApiResponse<CheckInBundleResponse> getBundle(@RequestParam String meetingId, @RequestParam String keyword) {
        return ApiResponse.success(participantService.getCheckInBundle(meetingId, keyword));
    }

    @PostMapping("/print")
    public ApiResponse<AttendanceResponse> markAsPrinted(@RequestParam String meetingId, @RequestParam String cccd) {
        return ApiResponse.success(participantService.markAsPrinted(meetingId, cccd));
    }
}
