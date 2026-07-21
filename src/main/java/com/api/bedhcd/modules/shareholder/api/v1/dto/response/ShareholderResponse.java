package com.api.bedhcd.modules.shareholder.api.v1.dto.response;

import java.time.LocalDateTime;

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
    private String meetingName;
    private Long attendingShares;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private LocalDateTime checkedInAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
