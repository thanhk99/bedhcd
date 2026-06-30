package com.api.bedhcd.modules.participant.domain.repository;

import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.shared.domain.enums.DelegationStatus;

import java.util.List;
import java.util.Optional;

public interface ProxyDelegationRepository {
    Optional<ProxyDelegation> findById(Long id);
    List<ProxyDelegation> findByMeetingId(String meetingId);
    List<ProxyDelegation> findByMeetingIdAndDelegatorId(String meetingId, String delegatorId, DelegationStatus status);
    List<ProxyDelegation> findByMeetingIdAndProxyId(String meetingId, String proxyId, DelegationStatus status);
    long sumReceivedProxyShares(String meetingId, String proxyId);
    long sumDelegatedShares(String meetingId, String delegatorId);
    ProxyDelegation save(ProxyDelegation delegation);
    void revokeById(Long id);
}
