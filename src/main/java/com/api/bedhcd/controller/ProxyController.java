package com.api.bedhcd.controller;

import com.api.bedhcd.dto.request.ProxyDelegationRequest;
import com.api.bedhcd.dto.response.ProxyDelegationResponse;
import com.api.bedhcd.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meetings/{meetingId}/proxy")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @PostMapping
    public ResponseEntity<ProxyDelegationResponse> createDelegation(@PathVariable String meetingId,
            @RequestBody ProxyDelegationRequest request) {
        return ResponseEntity.ok(proxyService.createDelegation(meetingId, request));
    }

    @GetMapping
    public ResponseEntity<List<ProxyDelegationResponse>> getDelegations(@PathVariable String meetingId) {
        return ResponseEntity.ok(proxyService.getDelegationsByMeeting(meetingId));
    }

    @PostMapping("/{delegationId}")
    public ResponseEntity<Void> revokeDelegation(@PathVariable Long delegationId) {
        proxyService.revokeDelegation(delegationId);
        return ResponseEntity.noContent().build();
    }
}
