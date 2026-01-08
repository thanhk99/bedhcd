package com.api.bedhcd.dto;

import com.api.bedhcd.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private String fullName;
    private Long sharesOwned;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private Long totalShares;
    private String phoneNumber;
    private String investorCode;
    private String cccd;
    private String dateOfIssue;
    private String placeOfIssue;
    private String address;
    private Set<Role> roles;
    private java.util.List<com.api.bedhcd.dto.response.ProxyDelegationResponse> delegationsMade;
    private java.util.List<com.api.bedhcd.dto.response.ProxyDelegationResponse> delegationsReceived;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
