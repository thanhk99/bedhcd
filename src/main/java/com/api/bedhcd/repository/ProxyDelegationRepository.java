package com.api.bedhcd.repository;

import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.enums.DelegationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProxyDelegationRepository extends JpaRepository<ProxyDelegation, Long>, JpaSpecificationExecutor<ProxyDelegation> {
        List<ProxyDelegation> findByMeeting_IdAndDelegator_Id(String meetingId, String delegatorId);

        List<ProxyDelegation> findByMeeting_IdAndDelegator_IdAndStatus(String meetingId, String delegatorId,
                        DelegationStatus status);

        List<ProxyDelegation> findByMeeting_IdAndProxy_Id(String meetingId, String proxyId);

        List<ProxyDelegation> findByMeeting_IdAndStatus(String meetingId, DelegationStatus status);

        List<ProxyDelegation> findByMeeting_Id(String meetingId);

        List<ProxyDelegation> findByMeeting_IdAndProxy_IdAndStatus(String meetingId, String proxyId,
                        DelegationStatus status);

        Optional<ProxyDelegation> findByMeeting_IdAndDelegator_IdAndProxy_IdAndStatus(String meetingId,
                        String delegatorId,
                        String proxyId, DelegationStatus status);

        List<ProxyDelegation> findByDelegator_Id(String delegatorId);

        List<ProxyDelegation> findByProxy_Id(String proxyId);

        boolean existsByMeeting_IdAndProxy_IdAndSharesDelegatedAndStatus(String meetingId, String proxyId, Long shares,
                        DelegationStatus status);

        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(pd.sharesDelegated), 0) FROM ProxyDelegation pd WHERE pd.meeting.id = :meetingId AND pd.proxy.id = :proxyId AND pd.status = com.api.bedhcd.entity.enums.DelegationStatus.ACTIVE")
        long sumReceivedProxyShares(String meetingId, String proxyId);

        @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(pd.sharesDelegated), 0) FROM ProxyDelegation pd WHERE pd.meeting.id = :meetingId AND pd.delegator.id = :delegatorId AND pd.status = com.api.bedhcd.entity.enums.DelegationStatus.ACTIVE")
        long sumDelegatedShares(String meetingId, String delegatorId);
}
