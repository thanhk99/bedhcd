package com.api.bedhcd.repository;

import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.enums.DelegationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyDelegationRepository extends JpaRepository<ProxyDelegation, Long> {
    Optional<ProxyDelegation> findByMeeting_IdAndDelegator_Id(String meetingId, String delegatorId);

    List<ProxyDelegation> findByMeeting_IdAndProxy_Id(String meetingId, String proxyId);

    List<ProxyDelegation> findByMeeting_IdAndStatus(String meetingId, DelegationStatus status);

    List<ProxyDelegation> findByDelegator_Id(String delegatorId);

    List<ProxyDelegation> findByProxy_Id(String proxyId);
}
