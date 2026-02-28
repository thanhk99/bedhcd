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
                long sharesToUpdate = request.getSharesDelegated();
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
                // Đảm bảo sharesOwned phản ánh tổng sở hữu từ User
                delegator.setSharesOwned(delegatorUser.getSharesOwned());
                proxy.setSharesOwned(proxyUser.getSharesOwned());

                // Cộng dồn vào số đã uỷ quyền (để theo dõi)
                delegator.setDelegatedShares(delegator.getDelegatedShares() + sharesToUpdate);
                // Cộng dồn vào số người được uỷ quyền nhận được
                proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() + sharesToUpdate);

                // Cập nhật attendingShares nếu đã điểm danh
                if (delegator.getCheckedInAt() != null) {
                        delegator.setAttendingShares(delegator.getAttendingShares() - sharesToUpdate);
                }
                if (proxy.getCheckedInAt() != null) {
                        proxy.setAttendingShares(proxy.getAttendingShares() + sharesToUpdate);
                }

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

                // Hoàn lại logic uỷ quyền
                long sharesToRevoke = delegation.getSharesDelegated();
                delegator.setDelegatedShares(delegator.getDelegatedShares() - sharesToRevoke);
                // Giảm số người được uỷ quyền nhận được
                proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() - sharesToRevoke);

                // Hoàn lại attendingShares nếu đã điểm danh
                if (delegator.getCheckedInAt() != null) {
                        delegator.setAttendingShares(delegator.getAttendingShares() + sharesToRevoke);
                }
                if (proxy.getCheckedInAt() != null) {
                        proxy.setAttendingShares(proxy.getAttendingShares() - sharesToRevoke);
                }

                meetingParticipantRepository.save(delegator);
                meetingParticipantRepository.save(proxy);
        }

        @Transactional
        public ProxyDelegationResponse updateDelegation(Long delegationId, Long newShares) {
                ProxyDelegation delegation = proxyDelegationRepository.findById(delegationId)
                                .orElseThrow(() -> new ResourceNotFoundException("Delegation not found"));

                if (delegation.getStatus() != DelegationStatus.ACTIVE) {
                        throw new BadRequestException("Cannot update inactive delegation");
                }

                long oldShares = delegation.getSharesDelegated();

                delegation.setSharesDelegated(newShares);
                proxyDelegationRepository.save(delegation);

                MeetingParticipant delegator = getOrCreateParticipant(delegation.getMeeting(),
                                delegation.getDelegator());
                MeetingParticipant proxy = getOrCreateParticipant(delegation.getMeeting(), delegation.getProxy());

                // Cập nhật số liệu
                long diff = newShares - oldShares;
                delegator.setDelegatedShares(delegator.getDelegatedShares() + diff);
                proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() + diff);

                // Đồng bộ attendingShares cho người uỷ quyền
                if (delegator.getCheckedInAt() != null) {
                        delegator.setAttendingShares(delegator.getAttendingShares() - diff);
                }

                // Nếu Proxy chưa điểm danh, tự động điểm danh luôn
                if (proxy.getCheckedInAt() == null) {
                        proxy.setCheckedInAt(LocalDateTime.now());
                        proxy.setStatus(com.api.bedhcd.entity.enums.ParticipantStatus.CHECKED_IN);
                        // Công thức tính attendingShares: sở hữu - đã uỷ quyền + nhận uỷ quyền
                        long initialAttending = (proxy.getSharesOwned() != null ? proxy.getSharesOwned() : 0L)
                                        - (proxy.getDelegatedShares() != null ? proxy.getDelegatedShares() : 0L)
                                        + (proxy.getReceivedProxyShares() != null ? proxy.getReceivedProxyShares()
                                                        : 0L);
                        proxy.setAttendingShares(initialAttending);
                } else {
                        // Nếu đã điểm danh rồi, cập nhật attendingShares theo mức chênh lệch
                        proxy.setAttendingShares(proxy.getAttendingShares() + diff);
                }

                meetingParticipantRepository.save(delegator);
                meetingParticipantRepository.save(proxy);

                return mapToResponse(delegation);
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
