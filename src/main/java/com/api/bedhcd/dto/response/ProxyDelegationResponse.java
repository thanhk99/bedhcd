package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.DelegationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyDelegationResponse {
    private Long id;
    private Long delegatorId;
    private String delegatorName;
    private Long proxyId;
    private String proxyName;
    private Integer sharesDelegated;
    private String authorizationDocument;
    private DelegationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
}
