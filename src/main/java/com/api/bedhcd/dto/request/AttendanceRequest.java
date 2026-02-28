package com.api.bedhcd.dto.request;

import com.api.bedhcd.entity.enums.ParticipationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    private String meetingId;
    private String investorCode;
    private Long attendingShares;
    private ParticipationType participationType;
}
