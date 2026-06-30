package com.api.bedhcd.modules.participant.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
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
    private Long sharesOwned;
    private Long attendingShares;
    private Long receivedProxyShares;
    private Long delegatedShares;
    private ParticipationType participationType;
    private ParticipantStatus status;
    private LocalDateTime checkedInAt;
}
