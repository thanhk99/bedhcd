package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitTicketRequest {
    private String proxyUserId;
    private List<TicketRequest> tickets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketRequest {
        /** Phần proxy TỰ THAM DỰ vào phiếu này (→ attendingShares của phiếu con) */
        private Long attendingShares;
        /** Các UỶ QUYỀN cụ thể gộp vào phiếu này (→ receivedProxyShares của phiếu con) */
        private List<Long> delegationIds;
    }
}
