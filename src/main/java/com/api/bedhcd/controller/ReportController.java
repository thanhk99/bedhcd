package com.api.bedhcd.controller;

import com.api.bedhcd.dto.response.ReportStatsResponse;
import com.api.bedhcd.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/voting-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportStatsResponse> getVotingStats(@RequestParam String meetingId) {
        return ResponseEntity.ok(reportService.getVotingReportStats(meetingId));
    }
}
