package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ParticipantRepositoryImpl implements ParticipantRepository {

    private final ParticipantJpaRepository jpaRepository;

    @Override
    public Optional<Participant> findByMeetingIdAndUserId(String meetingId, String userId) {
        return jpaRepository.findByMeetingIdAndUserId(meetingId, userId).map(this::toDomain);
    }

    @Override
    public java.util.List<Participant> findAllByMeetingIdAndUserIdIn(String meetingId, java.util.List<String> userIds) {
        return jpaRepository.findAllByMeetingIdAndUserIdIn(meetingId, userIds).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Participant> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Participant> findByMeetingId(String meetingId) {
        return jpaRepository.findByMeetingId(meetingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Participant> findCheckedInParticipants(String meetingId) {
        return jpaRepository.findByMeetingIdAndStatusInOrderByCheckedInAtDesc(
                meetingId,
                List.of(
                        ParticipantStatus.CHECKED_IN,
                        ParticipantStatus.PRINT))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Participant save(Participant domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public java.util.List<Participant> saveAll(java.util.List<Participant> domains) {
        java.util.List<ParticipantEntity> entities = domains.stream().map(this::toEntity).collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByCheckedIn(boolean checkedIn) {
        if (checkedIn) {
            return jpaRepository.countByMeetingIdAndStatusIn(null, List.of( // Tạm thời null cho toàn cục
                    ParticipantStatus.CHECKED_IN,
                    ParticipantStatus.PRINT));
        } else {
            return jpaRepository.countByMeetingIdAndStatusIn(null, List.of(ParticipantStatus.PENDING));
        }
    }

    @Override
    public long countByMeetingId(String meetingId) {
        return jpaRepository.countByMeetingId(meetingId);
    }

    @Override
    public long countCheckedInByMeetingId(String meetingId) {
        // Loại trừ các phiếu con từ tách phiếu (splitTicket = true) để không tính trùng người
        return jpaRepository.countCheckedInExcludingSplitTickets(meetingId, List.of(
                ParticipantStatus.CHECKED_IN,
                ParticipantStatus.PRINT));
    }

    @Override
    public long sumTotalShares() {
        return jpaRepository.sumTotalShares();
    }

    @Override
    public long sumCheckedInShares() {
        return jpaRepository.sumAttendingSharesByStatusIn(List.of(
                ParticipantStatus.CHECKED_IN,
                ParticipantStatus.PRINT));
    }

    @Override
    public long sumTotalSharesByMeetingId(String meetingId) {
        return jpaRepository.sumTotalSharesByMeetingId(meetingId);
    }

    @Override
    public long sumCheckedInSharesByMeetingId(String meetingId) {
        return jpaRepository.sumAttendingSharesByMeetingIdAndStatusIn(meetingId, List.of(
                ParticipantStatus.CHECKED_IN,
                ParticipantStatus.PRINT));
    }

    private Participant toDomain(ParticipantEntity entity) {
        return Participant.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .userId(entity.getUserId())
                .participationType(entity.getParticipationType())
                .status(entity.getStatus())
                .attendingShares(entity.getAttendingShares())
                .sharesOwned(entity.getSharesOwned())
                .receivedProxyShares(entity.getReceivedProxyShares())
                .delegatedShares(entity.getDelegatedShares())
                .checkedInAt(entity.getCheckedInAt())
                .splitTicket(entity.isSplitTicket())
                .build();
    }

    private ParticipantEntity toEntity(Participant domain) {
        return ParticipantEntity.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .userId(domain.getUserId())
                .participationType(domain.getParticipationType())
                .status(domain.getStatus())
                .attendingShares(domain.getAttendingShares())
                .sharesOwned(domain.getSharesOwned())
                .receivedProxyShares(domain.getReceivedProxyShares())
                .delegatedShares(domain.getDelegatedShares())
                .checkedInAt(domain.getCheckedInAt())
                .splitTicket(domain.isSplitTicket())
                .build();
    }
}
