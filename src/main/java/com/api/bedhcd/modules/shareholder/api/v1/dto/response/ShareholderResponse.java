package com.api.bedhcd.modules.shareholder.api.v1.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareholderResponse {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String cccd;
    private String investorCode;
    private Long sharesOwned;
    private boolean enabled;

    private String meetingId;
    private Long attendingShares;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private java.time.LocalDateTime checkedInAt;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
