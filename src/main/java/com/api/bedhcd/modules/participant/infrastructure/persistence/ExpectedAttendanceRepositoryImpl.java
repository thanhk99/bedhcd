package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.modules.participant.domain.model.ExpectedAttendance;
import com.api.bedhcd.modules.participant.domain.repository.ExpectedAttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExpectedAttendanceRepositoryImpl implements ExpectedAttendanceRepository {

    private final ExpectedAttendanceJpaRepository jpaRepository;

    @Override
    public List<ExpectedAttendance> findByMeetingId(String meetingId) {
        return jpaRepository.findByMeetingId(meetingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExpectedAttendance> saveAll(List<ExpectedAttendance> items) {
        List<ExpectedAttendanceEntity> entities = items.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByMeetingId(String meetingId) {
        jpaRepository.deleteByMeetingId(meetingId);
    }

    @Override
    public long countByMeetingId(String meetingId) {
        return jpaRepository.countByMeetingId(meetingId);
    }

    private ExpectedAttendance toDomain(ExpectedAttendanceEntity entity) {
        return ExpectedAttendance.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .cccd(entity.getCccd())
                .expectedShares(entity.getExpectedShares())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ExpectedAttendanceEntity toEntity(ExpectedAttendance domain) {
        return ExpectedAttendanceEntity.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .cccd(domain.getCccd())
                .expectedShares(domain.getExpectedShares())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
