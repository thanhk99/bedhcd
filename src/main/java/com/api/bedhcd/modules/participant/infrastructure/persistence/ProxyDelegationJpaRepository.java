package com.api.bedhcd.modules.participant.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProxyDelegationJpaRepository extends JpaRepository<ProxyDelegationEntity, Long> {

    List<ProxyDelegationEntity> findByMeetingId(String meetingId);

    List<ProxyDelegationEntity> findByMeetingIdAndDelegatorIdAndStatus(
            String meetingId, String delegatorId, DelegationStatus status);

    List<ProxyDelegationEntity> findByMeetingIdAndProxyIdAndStatus(
            String meetingId, String proxyId, DelegationStatus status);

    @Query("SELECT COALESCE(SUM(p.sharesDelegated), 0) FROM ProxyDelegationEntity p " +
           "WHERE p.meetingId = :meetingId AND p.proxyId = :proxyId AND p.status = 'ACTIVE'")
    long sumReceivedProxyShares(@Param("meetingId") String meetingId, @Param("proxyId") String proxyId);

    @Query("SELECT COALESCE(SUM(p.sharesDelegated), 0) FROM ProxyDelegationEntity p " +
           "WHERE p.meetingId = :meetingId AND p.delegatorId = :delegatorId AND p.status = 'ACTIVE'")
    long sumDelegatedShares(@Param("meetingId") String meetingId, @Param("delegatorId") String delegatorId);
}
