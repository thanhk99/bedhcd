package com.api.bedhcd.dto.response;

import com.api.bedhcd.entity.enums.ParticipationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private String userId;
    private String meetingId;
    private String investorCode;
    private String shareholderCode;
    private String fullName;
    private String cccd;
    private String dateOfIssue;
    private String placeOfIssue;
    private String phoneNumber;
    private String email;
    private Long sharesOwned;
    private Long attendingShares;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private ParticipationType participationType;
    private LocalDateTime checkedInAt;
}
