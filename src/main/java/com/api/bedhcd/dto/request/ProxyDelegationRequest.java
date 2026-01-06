package com.api.bedhcd.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyDelegationRequest {
    private Long delegatorId;
    private Long proxyId;
    private Integer sharesDelegated;
    private String authorizationDocument;
}
