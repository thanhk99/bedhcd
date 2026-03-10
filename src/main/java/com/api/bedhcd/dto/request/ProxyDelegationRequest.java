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
    private String delegatorId;
    private String proxyId;
    private Long sharesDelegated;
    private String authorizationDocument;
    private java.time.LocalDate authorizationDate;
}
