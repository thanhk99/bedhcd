package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDelegationRequest {
    private String delegatorId;
    private String proxyId;
    private Long sharesDelegated;
    private String authorizationDocument;
    private LocalDate authorizationDate;
}
