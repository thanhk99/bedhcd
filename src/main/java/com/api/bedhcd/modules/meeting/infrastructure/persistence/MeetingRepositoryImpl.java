package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.api.bedhcd.shared.domain.UuidFactory;

@Repository
@RequiredArgsConstructor
public class MeetingRepositoryImpl implements MeetingRepository {

    private final MeetingJpaRepository jpaRepository;

    @Override
    public Optional<Meeting> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public java.util.List<Meeting> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public java.util.List<Meeting> findAllOrderByCreatedAtDesc() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Optional<Meeting> findByMeetingCode(String code) {
        return jpaRepository.findByMeetingCode(code).map(this::toDomain);
    }

    @Override
    public Meeting save(Meeting domain) {
        if (domain.getId() == null || domain.getId().isEmpty()) {
            domain.setId(UuidFactory.generate());
        }
        MeetingEntity entity = toEntity(domain);
        MeetingEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    private Meeting toDomain(MeetingEntity entity) {
        return Meeting.builder()
                .id(entity.getId())
                .meetingCode(entity.getMeetingCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .location(entity.getLocation())
                .status(entity.getStatus())
                .configId(entity.getConfigId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MeetingEntity toEntity(Meeting domain) {
        return MeetingEntity.builder()
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
}
