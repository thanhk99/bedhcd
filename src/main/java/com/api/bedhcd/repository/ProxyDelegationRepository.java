package com.api.bedhcd.repository;

import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.enums.DelegationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyDelegationRepository extends JpaRepository<ProxyDelegation, Long> {
    Optional<ProxyDelegation> findByMeeting_IdAndDelegator_Id(Long meetingId, Long delegatorId);

    List<ProxyDelegation> findByMeeting_IdAndProxy_Id(Long meetingId, Long proxyId);

    List<ProxyDelegation> findByMeeting_IdAndStatus(Long meetingId, DelegationStatus status);
}
