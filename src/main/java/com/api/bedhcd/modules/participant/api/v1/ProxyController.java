package com.api.bedhcd.modules.participant.api.v1;

import com.api.bedhcd.modules.participant.api.v1.dto.ProxyDelegationRequest;
import com.api.bedhcd.modules.participant.api.v1.dto.ProxyDelegationResponse;
import com.api.bedhcd.modules.participant.application.service.ProxyApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proxy")
@Tag(name = "Quản lý uỷ quyền ", description = "API dành cho uỷ quyền")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyApplicationService proxyService;

    @Operation(summary = "Tạo uỷ quyền")
    @PostMapping("/{meetingId}")
    public ApiResponse<ProxyDelegationResponse> createDelegation(
            @PathVariable String meetingId,
            @RequestBody ProxyDelegationRequest request) {
        return ApiResponse.success(proxyService.createDelegation(meetingId, request));
    }

    @Operation(summary = "Lấy danh sách uỷ quyền")
    @GetMapping("/{meetingId}")
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

    @Operation(summary = "Thu hồi uỷ quyền")
    @PostMapping("/{meetingId}/{delegationId}/revoke")
    public ApiResponse<Void> revokeDelegation(
            @PathVariable String meetingId,
            @PathVariable Long delegationId) {
        proxyService.revokeDelegation(delegationId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Lấy danh sách uỷ quyền theo người uỷ quyền")
    @GetMapping("/{meetingId}/delegator/{userId}")
    public ApiResponse<List<ProxyDelegationResponse>> getProxiesByDelegator(
            @PathVariable String meetingId,
            @PathVariable String userId) {
        return ApiResponse.success(proxyService.getByDelegator(meetingId, userId));
    }

    @Operation(summary = "Lấy danh sách uỷ quyền theo người được uỷ quyền")
    @GetMapping("/{meetingId}/proxy/{userId}")
    public ApiResponse<List<ProxyDelegationResponse>> getProxiesByProxy(
            @PathVariable String meetingId,
            @PathVariable String userId) {
        return ApiResponse.success(proxyService.getByProxy(meetingId, userId));
    }

    @Operation(summary = "Cập nhật số cổ phần được uỷ quyền")
    @PutMapping("/{meetingId}/{delegationId}")
    public ApiResponse<ProxyDelegationResponse> updateDelegationShares(
            @PathVariable String meetingId,
            @PathVariable Long delegationId,
            @RequestBody java.util.Map<String, Long> payload) {
        long sharesDelegated = payload.getOrDefault("sharesDelegated", 0L);
        return ApiResponse.success(proxyService.updateDelegationShares(meetingId, delegationId, sharesDelegated));
    }

    @Operation(summary = "Tách phiếu bầu")
    @PostMapping("/{meetingId}/split-tickets")
    public ApiResponse<com.api.bedhcd.modules.participant.api.v1.dto.SplitTicketResponse> splitTickets(
            @PathVariable String meetingId,
            @RequestBody com.api.bedhcd.modules.participant.api.v1.dto.SplitTicketRequest request) {
        return ApiResponse.success(proxyService.createSplitTickets(meetingId, request));
    }
}
