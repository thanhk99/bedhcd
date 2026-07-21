package com.api.bedhcd.modules.shareholder.application.mapper;

import com.api.bedhcd.modules.shareholder.api.v1.dto.response.ShareholderResponse;
import com.api.bedhcd.modules.shareholder.domain.model.Shareholder;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShareholderMapper {

    private final ParticipantPort participantPort;
    private final MeetingPort meetingPort;

    public ShareholderResponse toResponse(Shareholder shareholder) {
        String meetingId = participantPort.getLastMeetingId(shareholder.getId());
        if (meetingId == null) {
            meetingId = meetingPort.getFallbackMeetingId(); // Fallback lấy cuộc họp mặc định
        }

        String meetingName = null;
        long attendingShares = 0L;
        long receivedProxyShares = 0L;
        long delegatedShares = 0L;
        java.time.LocalDateTime checkedInAt = null;

        if (meetingId != null) {
            meetingName = meetingPort.getMeetingName(meetingId);
            attendingShares = participantPort.getAttendingShares(meetingId, shareholder.getId());
            receivedProxyShares = participantPort.getReceivedProxyShares(meetingId, shareholder.getId());
            delegatedShares = participantPort.getDelegatedShares(meetingId, shareholder.getId());
            checkedInAt = participantPort.getCheckedInAt(meetingId, shareholder.getId());
        }

        return ShareholderResponse.builder()
                .id(shareholder.getId())
                .fullName(shareholder.getFullName())
                .email(shareholder.getEmail())
                .phoneNumber(shareholder.getPhoneNumber())
                .address(shareholder.getAddress())
                .cccd(shareholder.getCccd())
                .investorCode(shareholder.getInvestorCode())
                .sharesOwned(shareholder.getSharesOwned())
                .enabled(shareholder.isEnabled())
                .meetingId(meetingId)
                .meetingName(meetingName)
                .attendingShares(attendingShares)
                .receivedProxyShares(receivedProxyShares)
                .delegatedShares(delegatedShares)
                .checkedInAt(checkedInAt)
                .createdAt(shareholder.getCreatedAt())
                .updatedAt(shareholder.getUpdatedAt())
                .build();
    }
}
