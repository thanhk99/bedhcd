package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import com.api.bedhcd.modules.meeting.domain.model.EditRequestStatus;
import com.api.bedhcd.modules.meeting.domain.model.MeetingEditRequest;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingEditRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MeetingEditRequestRepositoryImpl implements MeetingEditRequestRepository {

    private final MeetingEditRequestJpaRepository jpaRepository;

    @Override
    public Optional<MeetingEditRequest> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<MeetingEditRequest> findByMeetingId(String meetingId) {
        return jpaRepository.findByMeetingId(meetingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<MeetingEditRequest> findAllPending() {
        return jpaRepository.findByStatus(EditRequestStatus.PENDING.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsPendingForMeeting(String meetingId) {
        return jpaRepository.existsByMeetingIdAndStatus(meetingId, EditRequestStatus.PENDING.name());
    }

    @Override
    public MeetingEditRequest save(MeetingEditRequest request) {
        MeetingEditRequestEntity entity = toEntity(request);
        MeetingEditRequestEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    // ─── Mapping ───────────────────────────────────────────────────────────────

    private MeetingEditRequest toDomain(MeetingEditRequestEntity entity) {
        return MeetingEditRequest.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .requestedBy(entity.getRequestedBy())
                .actionType(entity.getActionType())
                .status(EditRequestStatus.valueOf(entity.getStatus()))
                .description(entity.getDescription())
                .changes(entity.getChanges())
                .payload(entity.getPayload())
                .note(entity.getNote())
                .reviewedBy(entity.getReviewedBy())
                .createdAt(entity.getCreatedAt())
                .reviewedAt(entity.getReviewedAt())
                .build();
    }

    private MeetingEditRequestEntity toEntity(MeetingEditRequest domain) {
        return MeetingEditRequestEntity.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .requestedBy(domain.getRequestedBy())
                .actionType(domain.getActionType())
                .status(domain.getStatus().name())
                .description(domain.getDescription())
                .changes(domain.getChanges())
                .payload(domain.getPayload())
                .note(domain.getNote())
                .reviewedBy(domain.getReviewedBy())
                .createdAt(domain.getCreatedAt())
                .reviewedAt(domain.getReviewedAt())
                .build();
    }
}
