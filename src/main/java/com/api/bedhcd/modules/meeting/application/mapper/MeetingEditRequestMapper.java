package com.api.bedhcd.modules.meeting.application.mapper;

import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingEditRequestResponse;
import com.api.bedhcd.modules.meeting.domain.model.MeetingEditRequest;
import org.springframework.stereotype.Component;

@Component
public class MeetingEditRequestMapper {

    public MeetingEditRequestResponse toResponse(MeetingEditRequest domain) {
        if (domain == null) return null;
        return MeetingEditRequestResponse.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .requestedBy(domain.getRequestedBy())
                .actionType(domain.getActionType())
                .status(domain.getStatus().name())
                .payload(domain.getPayload())
                .note(domain.getNote())
                .reviewedBy(domain.getReviewedBy())
                .createdAt(domain.getCreatedAt())
                .reviewedAt(domain.getReviewedAt())
                .requiresApproval(domain.isPending())
                .build();
    }
}
