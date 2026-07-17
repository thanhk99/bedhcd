package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.api.v1.dto.ProxyDelegationRequest;
import com.api.bedhcd.modules.participant.api.v1.dto.ProxyDelegationResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.SplitTicketRequest;
import com.api.bedhcd.modules.participant.api.v1.dto.SplitTicketResponse;
import com.api.bedhcd.modules.participant.domain.exception.ParticipantException;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.modules.participant.domain.repository.ProxyDelegationRepository;
import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import com.api.bedhcd.shared.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProxyApplicationService {

        private final ProxyDelegationRepository proxyRepository;
        private final ParticipantRepository participantRepository;
        private final IdentityPort identityPort;
        private final MeetingPort meetingPort;

        @Transactional
        public ProxyDelegationResponse createDelegation(String meetingId, ProxyDelegationRequest request) {
                String meetingStatus = meetingPort.getStatus(meetingId);
                if (MeetingStatus.VOTING.equals(meetingStatus) || MeetingStatus.COMPLETED.equals(meetingStatus)) {
                        throw new RuntimeException(
                                        "Không thể tạo uỷ quyền khi cuộc họp đang biểu quyết hoặc đã kết thúc");
                }

                UserDTO delegatorUser = identityPort.getUserInfo(request.getDelegatorId());
                UserDTO proxyUser = identityPort.getUserInfo(request.getProxyId());

                Participant delegator = getOrCreateParticipant(meetingId, delegatorUser);
                Participant proxy = getOrCreateParticipant(meetingId, proxyUser);

                delegator.validateCanDelegate(request.getSharesDelegated());

                ProxyDelegation delegation = ProxyDelegation.builder()
                                .meetingId(meetingId)
                                .delegatorId(request.getDelegatorId())
                                .proxyId(request.getProxyId())
                                .sharesDelegated(request.getSharesDelegated())
                                .status(DelegationStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .build();

                delegation = proxyRepository.save(delegation);

                delegator.addDelegatedShares(request.getSharesDelegated());
                proxy.addReceivedProxyShares(request.getSharesDelegated());

                participantRepository.save(delegator);
                participantRepository.save(proxy);

                return mapToResponse(delegation, delegatorUser, proxyUser);
        }

        @Transactional
        public void revokeDelegation(Long delegationId) {
                ProxyDelegation delegation = proxyRepository.findById(delegationId)
                                .orElseThrow(() -> ParticipantException.notFound("Không tìm thấy uỷ quyền"));

                if (!delegation.isRevocable())
                        return;

                proxyRepository.revokeById(delegationId);

                Participant delegator = participantRepository
                                .findByMeetingIdAndUserId(delegation.getMeetingId(), delegation.getDelegatorId()).get();
                Participant proxy = participantRepository
                                .findByMeetingIdAndUserId(delegation.getMeetingId(), delegation.getProxyId()).get();

                delegator.adjustDelegatedShares(-delegation.getSharesDelegated());
                proxy.adjustReceivedProxyShares(-delegation.getSharesDelegated());

                participantRepository.save(delegator);
                participantRepository.save(proxy);
        }

        @Transactional(readOnly = true)
        public List<ProxyDelegationResponse> getByMeeting(String meetingId) {
                return proxyRepository.findByMeetingId(meetingId).stream()
                                .map(del -> mapToResponse(del,
                                                identityPort.getUserInfo(del.getDelegatorId()),
                                                identityPort.getUserInfo(del.getProxyId())))
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<ProxyDelegationResponse> getDelegationsPaginated(
                        String meetingId, int page, int size, String search, String status) {

                List<ProxyDelegationResponse> allResponses = getByMeeting(meetingId);

                java.util.stream.Stream<ProxyDelegationResponse> stream = allResponses.stream();

                // 1. Lọc theo status
                if (status != null && !status.equalsIgnoreCase("all")) {
                        stream = stream.filter(res -> res.getStatus() != null
                                        && res.getStatus().name().equalsIgnoreCase(status));
                }

                // 2. Lọc theo search
                if (search != null && !search.trim().isEmpty()) {
                        String lowerSearch = search.trim().toLowerCase();
                        stream = stream.filter(res -> (res.getDelegatorName() != null
                                        && res.getDelegatorName().toLowerCase().contains(lowerSearch)) ||
                                        (res.getDelegatorCccd() != null && res.getDelegatorCccd().contains(lowerSearch))
                                        ||
                                        (res.getProxyName() != null
                                                        && res.getProxyName().toLowerCase().contains(lowerSearch))
                                        ||
                                        (res.getProxyCccd() != null && res.getProxyCccd().contains(lowerSearch)));
                }

                List<ProxyDelegationResponse> filteredList = stream.collect(Collectors.toList());

                // 3. Phân trang
                int start = Math.min((int) org.springframework.data.domain.PageRequest.of(page, size).getOffset(),
                                filteredList.size());
                int end = Math.min((start + size), filteredList.size());

                List<ProxyDelegationResponse> subList = filteredList.subList(start, end);

                return new org.springframework.data.domain.PageImpl<>(
                                subList,
                                org.springframework.data.domain.PageRequest.of(page, size),
                                filteredList.size());
        }

        @Transactional(readOnly = true)
        public List<ProxyDelegationResponse> getByDelegator(String meetingId, String delegatorId) {
                return proxyRepository.findByMeetingIdAndDelegatorId(meetingId, delegatorId, null).stream()
                                .map(del -> mapToResponse(del,
                                                identityPort.getUserInfo(del.getDelegatorId()),
                                                identityPort.getUserInfo(del.getProxyId())))
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<ProxyDelegationResponse> getByProxy(String meetingId, String proxyId) {
                return proxyRepository.findByMeetingIdAndProxyId(meetingId, proxyId, null).stream()
                                .map(del -> mapToResponse(del,
                                                identityPort.getUserInfo(del.getDelegatorId()),
                                                identityPort.getUserInfo(del.getProxyId())))
                                .collect(Collectors.toList());
        }

        @Transactional
        public ProxyDelegationResponse updateDelegationShares(String meetingId, Long delegationId, long newShares) {
                ProxyDelegation delegation = proxyRepository.findById(delegationId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy uỷ quyền"));

                if (!delegation.getMeetingId().equals(meetingId)) {
                        throw new RuntimeException("Uỷ quyền không thuộc cuộc họp này");
                }

                delegation.validateEditable();

                long oldShares = delegation.getSharesDelegated();
                if (oldShares == newShares) {
                        return mapToResponse(delegation,
                                        identityPort.getUserInfo(delegation.getDelegatorId()),
                                        identityPort.getUserInfo(delegation.getProxyId()));
                }

                Participant delegator = participantRepository
                                .findByMeetingIdAndUserId(meetingId, delegation.getDelegatorId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin cổ đông uỷ quyền"));
                Participant proxy = participantRepository.findByMeetingIdAndUserId(meetingId, delegation.getProxyId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy thông tin người nhận uỷ quyền"));

                // Adjust delegator temporarily to validate remaining capacity
                long delta = newShares - oldShares;
                delegator.adjustDelegatedShares(-oldShares);
                delegator.validateCanDelegate(newShares);
                delegator.adjustDelegatedShares(oldShares); // Revert to let the next line apply correctly

                delegation.setSharesDelegated(newShares);
                proxyRepository.save(delegation);

                delegator.adjustDelegatedShares(delta);
                proxy.adjustReceivedProxyShares(delta);

                participantRepository.save(delegator);
                participantRepository.save(proxy);

                return mapToResponse(delegation,
                                identityPort.getUserInfo(delegator.getUserId()),
                                identityPort.getUserInfo(proxy.getUserId()));
        }

        /**
         * Tạo người nhận uỷ quyền không phải cổ đông (đại diện) và gán uỷ quyền.
         * Frontend gọi tới POST /api/representatives
         */
        @Transactional
        public Map<String, Object> createRepresentative(Map<String, Object> request) {
                String meetingId = (String) request.get("meetingId");
                String delegatorCccd = (String) request.get("delegatorCccd");
                String fullName = (String) request.get("fullName");
                String cccd = (String) request.get("cccd");
                long sharesDelegated = ((Number) request.get("sharesDelegated")).longValue();

                // Tạo hoặc lấy tài khoản đại diện (không phải cổ đông)
                // Không truyền sharesOwned để tránh ghi đè dữ liệu nếu người này đã là cổ đông
                UserDTO proxyUserDto = UserDTO.builder()
                                .cccd(cccd)
                                .fullName(fullName)
                                .investorCode(cccd)
                                .build();

                proxyUserDto = identityPort.createOrUpdateUser(proxyUserDto);

                // Tìm người uỷ quyền theo CCCD
                String delegatorId = identityPort.getUserIdByCccd(delegatorCccd)
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy người uỷ quyền với CCCD: " + delegatorCccd));

                // Tạo bản ghi uỷ quyền
                ProxyDelegationRequest delegationReq = new ProxyDelegationRequest();
                delegationReq.setDelegatorId(delegatorId);
                delegationReq.setProxyId(proxyUserDto.getId());
                delegationReq.setSharesDelegated(sharesDelegated);

                createDelegation(meetingId, delegationReq);

                // Trả về cấu trúc NonShareholderProxyResponse mà frontend expect
                Map<String, Object> response = new HashMap<>();
                response.put("id", proxyUserDto.getId());
                response.put("fullName", proxyUserDto.getFullName());
                response.put("cccd", proxyUserDto.getCccd());
                response.put("generatedPassword", proxyUserDto.getCccd());
                response.put("meetingId", meetingId);
                response.put("sharesDelegated", sharesDelegated);

                return response;
        }

        // ─── Private Helpers ─────────────────────────────────────────────────────

        private Participant getOrCreateParticipant(String meetingId, UserDTO user) {
                if (user == null) {
                        throw ParticipantException.badRequest("Thông tin người dùng không được để trống (null)");
                }
                return participantRepository.findByMeetingIdAndUserId(meetingId, user.getId())
                                .orElseGet(() -> {
                                        Participant p = Participant.builder()
                                                        .meetingId(meetingId)
                                                        .userId(user.getId())
                                                        .sharesOwned(user.getSharesOwned())
                                                        .status(ParticipantStatus.PENDING)
                                                        .participationType(ParticipationType.DIRECT)
                                                        .attendingShares(0L)
                                                        .receivedProxyShares(0L)
                                                        .delegatedShares(0L)
                                                        .build();
                                        return participantRepository.save(p);
                                });
        }

        private ProxyDelegationResponse mapToResponse(ProxyDelegation del, UserDTO delegator, UserDTO proxy) {
                return ProxyDelegationResponse.builder()
                                .id(del.getId())
                                .meetingId(del.getMeetingId())
                                .delegatorId(del.getDelegatorId())
                                .delegatorName(delegator != null ? delegator.getFullName() : null)
                                .delegatorCccd(delegator != null ? delegator.getCccd() : null)
                                .proxyId(del.getProxyId())
                                .proxyName(proxy != null ? proxy.getFullName() : null)
                                .proxyCccd(proxy != null ? proxy.getCccd() : null)
                                .sharesDelegated(del.getSharesDelegated())
                                .status(del.getStatus())
                                .createdAt(del.getCreatedAt())
                                .build();
        }

        @Transactional
        public SplitTicketResponse createSplitTickets(String meetingId, SplitTicketRequest request) {
                UserDTO proxyUser = identityPort.getUserInfo(request.getProxyUserId());
                Participant proxyParticipant = participantRepository
                                .findByMeetingIdAndUserId(meetingId, request.getProxyUserId())
                                .orElseThrow(() -> new RuntimeException("Proxy chưa check-in hoặc không tồn tại."));

                long totalProxyRights = (proxyParticipant.getAttendingShares() != null
                                ? proxyParticipant.getAttendingShares()
                                : 0L)
                                + (proxyParticipant.getReceivedProxyShares() != null
                                                ? proxyParticipant.getReceivedProxyShares()
                                                : 0L);

                long requestedTotal = request.getTickets().stream()
                                .mapToLong(SplitTicketRequest.TicketRequest::getShares)
                                .sum();

                if (requestedTotal > totalProxyRights) {
                        throw new RuntimeException(
                                        "Tổng số cổ phần tách (" + requestedTotal + ") lớn hơn tổng quyền biểu quyết ("
                                                        + totalProxyRights + ")");
                }

                if (request.getTickets().size() < 2) {
                        throw new RuntimeException("Phải tách thành ít nhất 2 phiếu.");
                }

                List<SplitTicketResponse.TicketResponse> responseTickets = new ArrayList<>();
                String baseCccd = proxyUser.getCccd();
                String baseInvestorCode = proxyUser.getInvestorCode() != null ? proxyUser.getInvestorCode() : baseCccd;

                for (int i = 0; i < request.getTickets().size(); i++) {
                        char suffixChar = (char) ('A' + i);
                        String ticketLabel = String.valueOf(suffixChar);
                        String newCccd = baseCccd + ticketLabel;
                        long shares = request.getTickets().get(i).getShares();

                        UserDTO subAccountDto = UserDTO.builder()
                                        .cccd(newCccd)
                                        .fullName(proxyUser.getFullName())
                                        .investorCode(baseInvestorCode + ticketLabel)
                                        .sharesOwned(shares)
                                        .splitAccount(true)
                                        .build();

                        subAccountDto = identityPort.createOrUpdateUser(subAccountDto);

                        // Tạo Participant cho sub-account (đánh dấu là splitTicket để không tính trùng
                        // số người)
                        Participant subParticipant = getOrCreateParticipant(meetingId, subAccountDto);
                        subParticipant.setAttendingShares(shares);
                        subParticipant.setParticipationType(ParticipationType.PROXY);
                        subParticipant.setStatus(ParticipantStatus.PRINT); // Đánh dấu là đã in
                        subParticipant.setCheckedInAt(LocalDateTime.now());
                        subParticipant.setSplitTicket(true); // Không tính vào số người tham dự
                        participantRepository.save(subParticipant);

                        responseTickets.add(SplitTicketResponse.TicketResponse.builder()
                                        .ticketLabel(ticketLabel)
                                        .cccd(newCccd)
                                        .shares(shares)
                                        .build());
                }

                // Cập nhật participant gốc để tránh bầu đúp
                proxyParticipant.setAttendingShares(0L);
                proxyParticipant.setReceivedProxyShares(0L);
                proxyParticipant.setStatus(ParticipantStatus.PRINT);
                participantRepository.save(proxyParticipant);

                return SplitTicketResponse.builder()
                                .tickets(responseTickets)
                                .build();
        }
}
