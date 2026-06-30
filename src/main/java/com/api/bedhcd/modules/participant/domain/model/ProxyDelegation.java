package com.api.bedhcd.modules.participant.domain.model;

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
public class ProxyDelegation {
    private Long id;
    private String meetingId;
    private String delegatorId;
    private String proxyId;
    private Long sharesDelegated;
    private DelegationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
}
