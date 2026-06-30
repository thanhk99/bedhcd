package com.api.bedhcd.modules.identity.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String meetingId;
    private String cccd;
    private String investorCode;
    private Long sharesOwned;
    private String phoneNumber;
    private String address;
    private Long attendingShares;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private Set<Role> roles;
    private boolean enabled;
    private java.time.LocalDateTime checkedInAt;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
