package com.api.bedhcd.modules.shareholder.application.mapper;

import com.api.bedhcd.modules.shareholder.api.v1.dto.response.ShareholderResponse;
import com.api.bedhcd.modules.shareholder.domain.model.Shareholder;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShareholderMapper {

    private final ParticipantPort participantPort;

    public ShareholderResponse toResponse(Shareholder shareholder) {
        String meetingId = participantPort.getLastMeetingId(shareholder.getId());
        long attendingShares = 0L;
        long receivedProxyShares = 0L;
        long delegatedShares = 0L;
        java.time.LocalDateTime checkedInAt = null;

        if (meetingId != null) {
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
                .attendingShares(attendingShares)
                .receivedProxyShares(receivedProxyShares)
                .delegatedShares(delegatedShares)
                .checkedInAt(checkedInAt)
                .createdAt(shareholder.getCreatedAt())
                .updatedAt(shareholder.getUpdatedAt())
                .build();
    }
}
