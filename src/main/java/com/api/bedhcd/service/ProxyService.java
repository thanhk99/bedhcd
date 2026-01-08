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
import com.api.bedhcd.repository.MeetingParticipantRepository;
import com.api.bedhcd.entity.MeetingParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProxyService {

        private final ProxyDelegationRepository proxyDelegationRepository;
        private final MeetingRepository meetingRepository;
        private final UserRepository userRepository;
        private final MeetingParticipantRepository meetingParticipantRepository;

        @SuppressWarnings("null")
        @Transactional
        public ProxyDelegationResponse createDelegation(String meetingId, ProxyDelegationRequest request) {
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                User delegatorUser = userRepository.findById(request.getDelegatorId())
                                .orElseThrow(() -> new ResourceNotFoundException("Delegator not found"));
                User proxyUser = userRepository.findById(request.getProxyId())
                                .orElseThrow(() -> new ResourceNotFoundException("Proxy not found"));

                MeetingParticipant delegator = getOrCreateParticipant(meeting, delegatorUser);
                MeetingParticipant proxy = getOrCreateParticipant(meeting, proxyUser);

                if (request.getSharesDelegated() > delegator.getSharesOwned()) {
                        throw new BadRequestException("Delegated shares exceed owned shares. Available: "
                                        + delegator.getSharesOwned());
                }

                // Kiểm tra xem đã có uỷ quyền ACTIVE cho người này chưa
                Optional<ProxyDelegation> existingDelegation = proxyDelegationRepository
                                .findByMeeting_IdAndDelegator_IdAndProxy_IdAndStatus(meetingId, delegatorUser.getId(),
                                                proxyUser.getId(), DelegationStatus.ACTIVE);

                ProxyDelegation delegation;
                long sharesToUpdate = request.getSharesDelegated(); // Store the shares from the current request

                if (existingDelegation.isPresent()) {
                        // GỘP: Cập nhật bản ghi cũ
                        delegation = existingDelegation.get();
                        delegation.setSharesDelegated(delegation.getSharesDelegated() + sharesToUpdate);
                        // Cập nhật tài liệu uỷ quyền nếu có gửi mới
                        if (request.getAuthorizationDocument() != null) {
                                delegation.setAuthorizationDocument(request.getAuthorizationDocument());
                        }
                } else {
                        // TẠO MỚI: Như cũ
                        delegation = ProxyDelegation.builder()
                                        .meeting(meeting)
                                        .delegator(delegatorUser)
                                        .proxy(proxyUser)
                                        .sharesDelegated(sharesToUpdate)
                                        .authorizationDocument(request.getAuthorizationDocument())
                                        .status(DelegationStatus.ACTIVE)
                                        .build();
                }

                delegation = proxyDelegationRepository.save(delegation);

                // Update share counts in MeetingParticipant
                // Giảm số sở hữu trực tiếp của người uỷ quyền trong cuộc họp này
                delegator.setSharesOwned(delegator.getSharesOwned() - sharesToUpdate);
                // Cộng dồn vào số đã uỷ quyền (để theo dõi)
                delegator.setDelegatedShares(delegator.getDelegatedShares() + sharesToUpdate);
                // Cộng dồn vào số người được uỷ quyền nhận được
                proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() + sharesToUpdate);

                meetingParticipantRepository.save(delegator);
                meetingParticipantRepository.save(proxy);

                return mapToResponse(delegation);
        }

        @Transactional
        public void revokeDelegation(Long delegationId) {
                ProxyDelegation delegation = proxyDelegationRepository.findById(delegationId)
                                .orElseThrow(() -> new ResourceNotFoundException("Delegation not found"));

                if (delegation.getStatus() == DelegationStatus.REVOKED) {
                        throw new BadRequestException("Delegation is already revoked");
                }

                delegation.setStatus(DelegationStatus.REVOKED);
                delegation.setRevokedAt(LocalDateTime.now());
                proxyDelegationRepository.save(delegation);

                // Update share counts in MeetingParticipant
                MeetingParticipant delegator = getOrCreateParticipant(delegation.getMeeting(),
                                delegation.getDelegator());
                MeetingParticipant proxy = getOrCreateParticipant(delegation.getMeeting(), delegation.getProxy());

                // Hoàn lại số sở hữu cho người uỷ quyền
                delegator.setSharesOwned(delegator.getSharesOwned() + delegation.getSharesDelegated());
                // Giảm số đã uỷ quyền
                delegator.setDelegatedShares(delegator.getDelegatedShares() - delegation.getSharesDelegated());
                // Giảm số người được uỷ quyền nhận được
                proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() - delegation.getSharesDelegated());

                meetingParticipantRepository.save(delegator);
                meetingParticipantRepository.save(proxy);
        }

        private MeetingParticipant getOrCreateParticipant(Meeting meeting, User user) {
                return meetingParticipantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), user.getId())
                                .orElseGet(() -> {
                                        MeetingParticipant participant = MeetingParticipant.builder()
                                                        .meeting(meeting)
                                                        .user(user)
                                                        .sharesOwned(user.getSharesOwned() != null
                                                                        ? user.getSharesOwned()
                                                                        : 0L)
                                                        .receivedProxyShares(0L)
                                                        .delegatedShares(0L)
                                                        .participationType(
                                                                        com.api.bedhcd.entity.enums.ParticipationType.DIRECT)
                                                        .status(com.api.bedhcd.entity.enums.ParticipantStatus.PENDING)
                                                        .build();
                                        return meetingParticipantRepository.save(participant);
                                });
        }

        @SuppressWarnings("null")
        public List<ProxyDelegationResponse> getDelegationsByMeeting(String meetingId) {
                return proxyDelegationRepository.findByMeeting_IdAndStatus(meetingId, DelegationStatus.ACTIVE).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        public List<ProxyDelegationResponse> getDelegationsByDelegator(String meetingId, String delegatorId) {
                return proxyDelegationRepository.findByMeeting_IdAndDelegator_Id(meetingId, delegatorId).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        public List<ProxyDelegationResponse> getDelegationsByProxy(String meetingId, String proxyId) {
                return proxyDelegationRepository.findByMeeting_IdAndProxy_Id(meetingId, proxyId).stream()
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
