package com.api.bedhcd.modules.participant.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDelegationResponse {
    private Long id;
    private String meetingId;
    private String delegatorId;
    private String delegatorName;
    private String delegatorCccd;
    private String proxyId;
    private String proxyName;
    private String proxyCccd;
    private boolean proxyIsRepresentative;
    private Long sharesDelegated;
    private DelegationStatus status;
    private LocalDateTime createdAt;
}
