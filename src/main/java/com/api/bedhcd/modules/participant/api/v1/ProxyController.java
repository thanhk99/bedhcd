package com.api.bedhcd.modules.participant.api.v1;

import com.api.bedhcd.modules.participant.api.v1.dto.ProxyDelegationRequest;
import com.api.bedhcd.modules.participant.api.v1.dto.ProxyDelegationResponse;
import com.api.bedhcd.modules.participant.application.service.ProxyApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/participant/meetings/{meetingId}/proxies")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyApplicationService proxyService;

    @PostMapping
    public ApiResponse<ProxyDelegationResponse> createDelegation(
            @PathVariable String meetingId,
            @RequestBody ProxyDelegationRequest request) {
        return ApiResponse.success(proxyService.createDelegation(meetingId, request));
    }

    @GetMapping
    public ApiResponse<?> getDelegations(
            @PathVariable String meetingId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String status) {
        if (page == null || size == null) {
            return ApiResponse.success(proxyService.getByMeeting(meetingId));
        }
        return ApiResponse.success(proxyService.getDelegationsPaginated(meetingId, page, size, search, status));
    }

    @PostMapping("/{delegationId}/revoke")
    public ApiResponse<Void> revokeDelegation(@PathVariable Long delegationId) {
        proxyService.revokeDelegation(delegationId);
        return ApiResponse.success(null);
    }

    @GetMapping("/delegator/{userId}")
    public ApiResponse<List<ProxyDelegationResponse>> getProxiesByDelegator(
            @PathVariable String meetingId,
            @PathVariable String userId) {
        return ApiResponse.success(proxyService.getByDelegator(meetingId, userId));
    }

    @GetMapping("/proxy/{userId}")
    public ApiResponse<List<ProxyDelegationResponse>> getProxiesByProxy(
            @PathVariable String meetingId,
            @PathVariable String userId) {
        return ApiResponse.success(proxyService.getByProxy(meetingId, userId));
    }

    @PutMapping("/{delegationId}")
    public ApiResponse<ProxyDelegationResponse> updateDelegationShares(
            @PathVariable String meetingId,
            @PathVariable Long delegationId,
            @RequestBody java.util.Map<String, Long> payload) {
        long sharesDelegated = payload.getOrDefault("sharesDelegated", 0L);
        return ApiResponse.success(proxyService.updateDelegationShares(meetingId, delegationId, sharesDelegated));
    }
}
