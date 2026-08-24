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
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import com.api.bedhcd.shared.domain.enums.Role;
import com.api.bedhcd.shared.dto.UserDTO;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProxyApplicationService {

        private final ProxyDelegationRepository proxyRepository;
        private final ParticipantRepository participantRepository;
        private final IdentityPort identityPort;
        private final MeetingPort meetingPort;
        private final VotingPort votingPort;

        @Transactional
        public ProxyDelegationResponse createDelegation(String meetingId, ProxyDelegationRequest request) {
                if (!meetingPort.shareholderCanAction(meetingId)) {
                        throw new RuntimeException(
                                        "Cấu hình cuộc họp hiện tại không cho phép thực hiện đăng ký tham dự hoặc ủy quyền.");
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

                resetAndInvalidateVotes(meetingId, proxy);
                mergeAndDeleteSplitTickets(meetingId, proxy);
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

                // Fix Bug 2: Nếu người nhận uỷ quyền không còn quyền biểu quyết nào
                // (thường là đại diện không phải cổ đông), reset về PENDING
                long totalVotingPower = (proxy.getAttendingShares() != null ? proxy.getAttendingShares() : 0L)
                                + (proxy.getReceivedProxyShares() != null ? proxy.getReceivedProxyShares() : 0L);
                if (totalVotingPower <= 0) {
                        proxy.setStatus(ParticipantStatus.PENDING);
                        proxy.setAttendingShares(0L);
                        proxy.setCheckedInAt(null);
                }

                participantRepository.save(delegator);

                resetAndInvalidateVotes(delegation.getMeetingId(), proxy);
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

                resetAndInvalidateVotes(meetingId, proxy);
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

                Optional<String> existingUserId = identityPort.getUserIdByCccd(cccd);
                boolean isRealShareholder = existingUserId
                                .map(identityPort::getUserInfo)
                                .map(this::isRealShareholder)
                                .orElse(false);

                UserDTO proxyUserDto;
                if (isRealShareholder) {
                        // Người này đã là cổ đông thật: giữ nguyên dữ liệu, chỉ dùng làm người nhận uỷ quyền
                        proxyUserDto = identityPort.getUserInfo(existingUserId.get());
                } else {
                        // Tạo/cập nhật tài khoản đại diện (không phải cổ đông):
                        // splitAccount=true để loại khỏi mọi danh sách/tính toán cổ đông,
                        // role=REPRESENTATIVE để nhận diện người đại diện
                        proxyUserDto = identityPort.createOrUpdateUser(UserDTO.builder()
                                        .cccd(cccd)
                                        .fullName(fullName)
                                        .roles(Set.of(Role.REPRESENTATIVE))
                                        .splitAccount(true)
                                        .sharesOwned(0L) // Fix Bug 1: Bắt buộc reset về 0, tránh giữ cổ phần cũ trong DB
                                        .build());
                }

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

                // Người nhận uỷ quyền (đại diện) tham gia cuộc họp với tư cách PROXY
                if (!isRealShareholder) {
                        participantRepository.findByMeetingIdAndUserId(meetingId, proxyUserDto.getId()).ifPresent(p -> {
                                p.setParticipationType(ParticipationType.PROXY);
                                participantRepository.save(p);
                        });
                }

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

        private boolean isRealShareholder(UserDTO user) {
                if (user == null) {
                        return false;
                }
                return !user.isSplitAccount()
                                && user.getSharesOwned() != null
                                && user.getSharesOwned() > 0;
        }

        // ─── Private Helpers ─────────────────────────────────────────────────────

        private void resetAndInvalidateVotes(String meetingId, Participant proxy) {
                boolean wasReset = proxy.resetToPendingPrint();
                if (wasReset) {
                        // Xoá toàn bộ phiếu bầu để buộc bỏ phiếu lại với quyền biểu quyết mới
                        votingPort.deleteVotesByMeetingAndUser(meetingId, proxy.getUserId());
                }
        }

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

        /**
         * Nghiệp vụ: Khi proxy nhận thêm uỷ quyền mới, gộp toàn bộ quyền đang nằm ở
         * các phiếu con (split ticket) trở lại proxy, xoá phiếu bầu + participant
         * của phiếu con để chỉ còn 1 phiếu tập trung.
         */
        private void mergeAndDeleteSplitTickets(String meetingId, Participant proxy) {
                UserDTO proxyUser = identityPort.getUserInfo(proxy.getUserId());
                if (proxyUser.getCccd() == null || proxyUser.getCccd().isBlank()) {
                        return;
                }
                List<UserDTO> splitAccounts = identityPort.findSplitAccountsByBaseCccd(proxyUser.getCccd());
                if (splitAccounts.isEmpty()) {
                        return;
                }
                List<String> splitUserIds = splitAccounts.stream()
                                .map(UserDTO::getId)
                                .collect(Collectors.toList());
                List<Participant> tickets = participantRepository.findAllByMeetingIdAndUserIdIn(meetingId,
                                splitUserIds);
                if (tickets.isEmpty()) {
                        return;
                }

                long attending = 0L;
                long received = 0L;
                for (Participant ticket : tickets) {
                        attending += ticket.getAttendingShares() != null ? ticket.getAttendingShares() : 0L;
                        received += ticket.getReceivedProxyShares() != null ? ticket.getReceivedProxyShares() : 0L;
                        resetAndInvalidateVotes(meetingId, ticket);
                        participantRepository.delete(ticket);
                }
                proxy.adjustAttendingShares(attending);
                proxy.adjustReceivedProxyShares(received);
        }

        private ProxyDelegationResponse mapToResponse(ProxyDelegation del, UserDTO delegator, UserDTO proxy) {                return ProxyDelegationResponse.builder()
                                .id(del.getId())
                                .meetingId(del.getMeetingId())
                                .delegatorId(del.getDelegatorId())
                                .delegatorName(delegator != null ? delegator.getFullName() : null)
                                .delegatorCccd(delegator != null ? delegator.getCccd() : null)
                                .proxyId(del.getProxyId())
                                .proxyName(proxy != null ? proxy.getFullName() : null)
                                .proxyCccd(proxy != null ? proxy.getCccd() : null)
                                .proxyIsRepresentative(proxy != null && proxy.isSplitAccount())
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

                long attendingShares = proxyParticipant.getAttendingShares() != null
                                ? proxyParticipant.getAttendingShares()
                                : 0L;
                long receivedShares = proxyParticipant.getReceivedProxyShares() != null
                                ? proxyParticipant.getReceivedProxyShares()
                                : 0L;
                long totalProxyRights = attendingShares + receivedShares;

                if (request.getTickets().size() < 2) {
                        throw new RuntimeException("Phải tách thành ít nhất 2 phiếu.");
                }

                // Tải toàn bộ uỷ quyền đang hoạt động của proxy
                Map<Long, ProxyDelegation> activeDelegations = proxyRepository
                                .findByMeetingIdAndProxyId(meetingId, request.getProxyUserId(),
                                                DelegationStatus.ACTIVE)
                                .stream()
                                .collect(Collectors.toMap(ProxyDelegation::getId, d -> d, (d1, d2) -> d1));

                // Validate nguồn: không trùng delegation giữa các phiếu, phải thuộc proxy + ACTIVE
                Set<Long> usedDelegationIds = new java.util.HashSet<>();
                long totalAttendingRequested = 0L;
                long totalReceivedRequested = 0L;
                for (SplitTicketRequest.TicketRequest ticket : request.getTickets()) {
                        long att = ticket.getAttendingShares() != null ? ticket.getAttendingShares() : 0L;
                        totalAttendingRequested += att;

                        if (ticket.getDelegationIds() != null) {
                                for (Long delegationId : ticket.getDelegationIds()) {
                                        ProxyDelegation delegation = activeDelegations.get(delegationId);
                                        if (delegation == null) {
                                                throw new RuntimeException(
                                                                "Uỷ quyền " + delegationId
                                                                                + " không thuộc người nhận uỷ quyền này hoặc không còn hiệu lực.");
                                        }
                                        if (!usedDelegationIds.add(delegationId)) {
                                                throw new RuntimeException(
                                                                "Uỷ quyền " + delegationId
                                                                                + " bị khai trùng ở nhiều phiếu.");
                                        }
                                        totalReceivedRequested += delegation.getSharesDelegated() != null
                                                        ? delegation.getSharesDelegated()
                                                        : 0L;
                                }
                        }
                }

                if (totalAttendingRequested > attendingShares) {
                        throw new RuntimeException(
                                        "Tổng cổ phần tham dự tách (" + totalAttendingRequested
                                                        + ") lớn hơn cổ phần tự tham dự của người uỷ quyền ("
                                                        + attendingShares + ")");
                }

                if (totalAttendingRequested + totalReceivedRequested != totalProxyRights) {
                        throw new RuntimeException(
                                        "Phải tách hết toàn bộ quyền biểu quyết (" + totalProxyRights
                                                        + "). Đã phân bổ: "
                                                        + (totalAttendingRequested + totalReceivedRequested));
                }

                List<SplitTicketResponse.TicketResponse> responseTickets = new ArrayList<>();
                String baseCccd = proxyUser.getCccd();
                String baseInvestorCode = proxyUser.getInvestorCode() != null ? proxyUser.getInvestorCode() : baseCccd;

                for (int i = 0; i < request.getTickets().size(); i++) {
                        char suffixChar = (char) ('A' + i);
                        String ticketLabel = String.valueOf(suffixChar);
                        String newCccd = baseCccd + ticketLabel;
                        SplitTicketRequest.TicketRequest ticket = request.getTickets().get(i);
                        long ticketAttending = ticket.getAttendingShares() != null
                                        ? ticket.getAttendingShares()
                                        : 0L;
                        long ticketReceived = 0L;
                        if (ticket.getDelegationIds() != null) {
                                for (Long delegationId : ticket.getDelegationIds()) {
                                        ticketReceived += activeDelegations.get(delegationId).getSharesDelegated() != null
                                                        ? activeDelegations.get(delegationId).getSharesDelegated()
                                                        : 0L;
                                }
                        }

                        UserDTO subAccountDto = UserDTO.builder()
                                        .cccd(newCccd)
                                        .fullName(proxyUser.getFullName())
                                        .investorCode(baseInvestorCode + ticketLabel)
                                        .sharesOwned(0L) // Phiếu con không sở hữu cổ phần - tránh đếm trùng
                                        .splitAccount(true)
                                        .build();

                        subAccountDto = identityPort.createOrUpdateUser(subAccountDto);

                        // Tạo Participant cho sub-account (đánh dấu là splitTicket để không tính trùng
                        // số người). Nếu phiếu con đã tồn tại (tách lại) thì reset + xoá phiếu bầu cũ.
                        Participant subParticipant = getOrCreateParticipant(meetingId, subAccountDto);
                        resetAndInvalidateVotes(meetingId, subParticipant);
                        subParticipant.setAttendingShares(ticketAttending);
                        subParticipant.setReceivedProxyShares(ticketReceived);
                        subParticipant.setParticipationType(ParticipationType.PROXY);
                        subParticipant.setStatus(ParticipantStatus.PRINT); // Đánh dấu là đã in
                        subParticipant.setCheckedInAt(LocalDateTime.now());
                        subParticipant.setSplitTicket(true); // Không tính vào số người tham dự
                        participantRepository.save(subParticipant);

                        responseTickets.add(SplitTicketResponse.TicketResponse.builder()
                                        .ticketLabel(ticketLabel)
                                        .cccd(newCccd)
                                        .attendingShares(ticketAttending)
                                        .receivedProxyShares(ticketReceived)
                                        .build());
                }

                // Cập nhật participant gốc để tránh bầu đúp
                // Reset về CHECKED_IN + xoá phiếu bầu cũ khi quyền biểu quyết thay đổi
                resetAndInvalidateVotes(meetingId, proxyParticipant);
                proxyParticipant.setAttendingShares(0L);
                proxyParticipant.setReceivedProxyShares(0L);
                proxyParticipant.setStatus(ParticipantStatus.PRINT);
                participantRepository.save(proxyParticipant);

                return SplitTicketResponse.builder()
                                .tickets(responseTickets)
                                .build();
        }
}
