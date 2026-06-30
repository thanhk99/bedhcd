package com.api.bedhcd.modules.participant.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.ParticipationType;
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
    private String cccd;
    private Long attendingShares;
    private ParticipationType participationType;
}
