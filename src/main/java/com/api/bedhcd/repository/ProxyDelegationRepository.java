package com.api.bedhcd.repository;

import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.enums.DelegationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyDelegationRepository extends JpaRepository<ProxyDelegation, Long> {
    Optional<ProxyDelegation> findByMeetingIdAndDelegatorId(Long meetingId, Long delegatorId);

    List<ProxyDelegation> findByMeetingIdAndProxyId(Long meetingId, Long proxyId);

    List<ProxyDelegation> findByMeetingIdAndStatus(Long meetingId, DelegationStatus status);
}
