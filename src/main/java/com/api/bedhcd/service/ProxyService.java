package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.ProxyDelegationRequest;
import com.api.bedhcd.dto.response.ProxyDelegationResponse;
import com.api.bedhcd.entity.Meeting;
import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.entity.enums.DelegationStatus;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.MeetingRepository;
import com.api.bedhcd.repository.ProxyDelegationRepository;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ProxyDelegationRepository proxyDelegationRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProxyDelegationResponse createDelegation(Long meetingId, ProxyDelegationRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        User delegator = userRepository.findById(request.getDelegatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Delegator not found"));
        User proxy = userRepository.findById(request.getProxyId())
                .orElseThrow(() -> new ResourceNotFoundException("Proxy not found"));

        if (proxyDelegationRepository.findByMeeting_IdAndDelegator_Id(meetingId, delegator.getId()).isPresent()) {
            throw new BadRequestException("User has already delegated for this meeting");
        }

        if (request.getSharesDelegated() > delegator.getSharesOwned()) {
            throw new BadRequestException("Delegated shares exceed owned shares");
        }

        ProxyDelegation delegation = ProxyDelegation.builder()
                .meeting(meeting)
                .delegator(delegator)
                .proxy(proxy)
                .sharesDelegated(request.getSharesDelegated())
                .authorizationDocument(request.getAuthorizationDocument())
                .status(DelegationStatus.ACTIVE)
                .build();

        delegation = proxyDelegationRepository.save(delegation);

        // Update share counts
        delegator.setDelegatedShares(delegator.getDelegatedShares() + delegation.getSharesDelegated());
        proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() + delegation.getSharesDelegated());
        userRepository.save(delegator);
        userRepository.save(proxy);

        return mapToResponse(delegation);
    }

    @Transactional
    public void revokeDelegation(Long delegationId) {
        ProxyDelegation delegation = proxyDelegationRepository.findById(delegationId)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation not found"));

        delegation.setStatus(DelegationStatus.REVOKED);
        delegation.setRevokedAt(LocalDateTime.now());
        proxyDelegationRepository.save(delegation);

        // Update share counts
        User delegator = delegation.getDelegator();
        User proxy = delegation.getProxy();
        delegator.setDelegatedShares(delegator.getDelegatedShares() - delegation.getSharesDelegated());
        proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() - delegation.getSharesDelegated());
        userRepository.save(delegator);
        userRepository.save(proxy);
    }

    public List<ProxyDelegationResponse> getDelegationsByMeeting(Long meetingId) {
        return proxyDelegationRepository.findByMeeting_IdAndStatus(meetingId, DelegationStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProxyDelegationResponse mapToResponse(ProxyDelegation delegation) {
        return ProxyDelegationResponse.builder()
                .id(delegation.getId())
                .delegatorId(delegation.getDelegator().getId())
                .delegatorName(delegation.getDelegator().getFullName())
                .proxyId(delegation.getProxy().getId())
                .proxyName(delegation.getProxy().getFullName())
                .sharesDelegated(delegation.getSharesDelegated())
                .authorizationDocument(delegation.getAuthorizationDocument())
                .status(delegation.getStatus())
                .createdAt(delegation.getCreatedAt())
                .revokedAt(delegation.getRevokedAt())
                .build();
    }
}
