package com.api.bedhcd.modules.meeting.application.mapper;

import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingResponse;
import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import com.api.bedhcd.modules.meeting.domain.model.MeetingRules;
import org.springframework.stereotype.Component;

@Component
public class MeetingMapper {

    public MeetingResponse toResponse(Meeting domain) {
        if (domain == null) return null;
        return MeetingResponse.builder()
                .id(domain.getId())
                .meetingCode(domain.getMeetingCode())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .location(domain.getLocation())
                .status(domain.getStatus())
                .configId(domain.getConfigId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public MeetingResponse toResponse(Meeting domain, MeetingConfig config) {
        MeetingResponse response = toResponse(domain);
        if (config != null) {
            MeetingRules rules = config.getRulesForStatus(domain.getStatus());
            if (rules != null && rules.getName() != null) {
                response.setStatusName(rules.getName());
            }
        }
        return response;
    }
}
