package com.api.bedhcd.modules.meeting.application.mapper;

import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingEditRequestResponse;
import com.api.bedhcd.modules.meeting.domain.model.MeetingEditRequest;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingEditRequestMapper {

    private final AdminJpaRepository adminJpaRepository;

    public MeetingEditRequestResponse toResponse(MeetingEditRequest domain) {
        if (domain == null) return null;
        
        String requestedByName = adminJpaRepository.findById(domain.getRequestedBy())
                .map(admin -> admin.getFullName() != null ? admin.getFullName() : admin.getUsername())
                .orElse(null);

        return MeetingEditRequestResponse.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .requestedBy(domain.getRequestedBy())
                .requestedByName(requestedByName)
                .actionType(domain.getActionType())
                .status(domain.getStatus().name())
                .description(domain.getDescription())
                .changes(domain.getChanges())
                .payload(domain.getPayload())
                .note(domain.getNote())
                .reviewedBy(domain.getReviewedBy())
                .createdAt(domain.getCreatedAt())
                .reviewedAt(domain.getReviewedAt())
                .requiresApproval(domain.isPending())
                .build();
    }
}
