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
public class CheckInBundleResponse {
    private AttendanceResponse shareholder;
    private List<ProxyAttendeeDTO> outgoingProxies;
    private List<IncomingProxyDTO> incomingProxies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomingProxyDTO {
        private Long delegationId;
        private Long sharesDelegated;
        private AttendanceResponse delegatorParticipant;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProxyAttendeeDTO {
        private Long delegationId;
        private Long sharesDelegated;
        private AttendanceResponse proxyParticipant;
    }
}
