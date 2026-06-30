package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.modules.participant.domain.repository.ProxyDelegationRepository;
import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProxyDelegationRepositoryImpl implements ProxyDelegationRepository {

    private final ProxyDelegationJpaRepository jpaRepository;

    @Override
    public Optional<ProxyDelegation> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProxyDelegation> findByMeetingId(String meetingId) {
        return jpaRepository.findByMeetingId(meetingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProxyDelegation> findByMeetingIdAndDelegatorId(String meetingId, String delegatorId, DelegationStatus status) {
        return jpaRepository.findByMeetingIdAndDelegatorIdAndStatus(meetingId, delegatorId, status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProxyDelegation> findByMeetingIdAndProxyId(String meetingId, String proxyId, DelegationStatus status) {
        return jpaRepository.findByMeetingIdAndProxyIdAndStatus(meetingId, proxyId, status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long sumReceivedProxyShares(String meetingId, String proxyId) {
        return jpaRepository.sumReceivedProxyShares(meetingId, proxyId);
    }

    @Override
    public long sumDelegatedShares(String meetingId, String delegatorId) {
        return jpaRepository.sumDelegatedShares(meetingId, delegatorId);
    }

    @Override
    public ProxyDelegation save(ProxyDelegation domain) {
        ProxyDelegationEntity entity = toEntity(domain);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void revokeById(Long id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(DelegationStatus.REVOKED);
            entity.setRevokedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    private ProxyDelegation toDomain(ProxyDelegationEntity entity) {
        return ProxyDelegation.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .delegatorId(entity.getDelegatorId())
                .proxyId(entity.getProxyId())
                .sharesDelegated(entity.getSharesDelegated())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    private ProxyDelegationEntity toEntity(ProxyDelegation domain) {
        return ProxyDelegationEntity.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .delegatorId(domain.getDelegatorId())
                .proxyId(domain.getProxyId())
                .sharesDelegated(domain.getSharesDelegated())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .revokedAt(domain.getRevokedAt())
                .build();
    }
}
