package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.api.v1.dto.AttendanceRequest;
import com.api.bedhcd.modules.participant.api.v1.dto.AttendanceResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.ReconciliationItemResponse;
import com.api.bedhcd.modules.participant.api.v1.dto.ReconciliationResponse;
import com.api.bedhcd.modules.participant.domain.exception.ParticipantException;
import com.api.bedhcd.modules.participant.api.v1.dto.CheckInBundleResponse;
import com.api.bedhcd.modules.participant.domain.model.ExpectedAttendance;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.modules.participant.domain.repository.ExpectedAttendanceRepository;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.modules.participant.domain.repository.ProxyDelegationRepository;
import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import com.api.bedhcd.shared.dto.PageResponse;
import com.api.bedhcd.shared.dto.UserDTO;
import com.api.bedhcd.shared.dto.importing.ExpectedAttendanceImportRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipantApplicationService {

        private final ParticipantRepository participantRepository;
        private final ProxyDelegationRepository proxyDelegationRepository;
        private final ExpectedAttendanceRepository expectedAttendanceRepository;
        private final ReconciliationService reconciliationService;
        private final IdentityPort identityPort;
        private final MeetingPort meetingPort;

        @Transactional
        public AttendanceResponse registerAttendance(AttendanceRequest request) {
                if (!meetingPort.canAttend(request.getMeetingId())) {
                        throw ParticipantException.invalidState(
                                        "Cấu hình hiện tại của cuộc họp không cho phép điểm danh hoặc chưa thiết lập quy tắc.");
                }

                String userId = identityPort.getUserIdByCccd(request.getCccd())
                                .orElseThrow(() -> ParticipantException.notFound(
                                                "Không tìm thấy cổ đông với CCCD: " + request.getCccd()));

                UserDTO user = identityPort.getUserInfo(userId);

                Participant participant = participantRepository.findByMeetingIdAndUserId(request.getMeetingId(), userId)
                                .orElse(Participant.builder()
                                                .meetingId(request.getMeetingId())
                                                .userId(userId)
                                                .build());

                // Đồng bộ số liệu cổ phần từ các nguồn ngoài (uỷ quyền, sở hữu)
                long ownedShares = user.getSharesOwned() != null ? user.getSharesOwned() : 0L;
                long receivedProxyShares = proxyDelegationRepository.sumReceivedProxyShares(request.getMeetingId(),
                                userId);
                long delegatedShares = proxyDelegationRepository.sumDelegatedShares(request.getMeetingId(), userId);

                participant.syncShares(ownedShares, receivedProxyShares, delegatedShares);

                // Toàn bộ logic nghiệp vụ điểm danh được uỷ thác cho Domain Model
                participant.checkIn(request.getAttendingShares());

                return mapToResponse(participantRepository.save(participant), user);
        }

        @Transactional
        public AttendanceResponse updateAttendance(AttendanceRequest request) {
                if (!meetingPort.canAttend(request.getMeetingId())) {
                        throw ParticipantException.invalidState(
                                        "Cấu hình hiện tại của cuộc họp không cho phép điểm danh hoặc chưa thiết lập quy tắc.");
                }

                String userId = identityPort.getUserIdByCccd(request.getCccd())
                                .orElseThrow(() -> ParticipantException
                                                .notFound("Không tìm thấy cổ đông với CCCD: " + request.getCccd()));

                UserDTO user = identityPort.getUserInfo(userId);

                Participant participant = participantRepository.findByMeetingIdAndUserId(request.getMeetingId(), userId)
                                .orElseThrow(() -> ParticipantException.notFound("Chưa có bản ghi tham dự"));

                long ownedShares = user.getSharesOwned() != null ? user.getSharesOwned() : 0L;
                long receivedProxyShares = proxyDelegationRepository.sumReceivedProxyShares(request.getMeetingId(),
                                userId);
                long delegatedShares = proxyDelegationRepository.sumDelegatedShares(request.getMeetingId(), userId);

                participant.syncShares(ownedShares, receivedProxyShares, delegatedShares);

                participant.updateCheckIn(request.getAttendingShares());

                return mapToResponse(participantRepository.save(participant), user);
        }

        @Transactional(readOnly = true)
        public PageResponse<AttendanceResponse> getAttendedParticipants(String meetingId, int page, int size,
                        String keyword) {
                List<AttendanceResponse> items = participantRepository
                                .findCheckedInParticipants(meetingId, page, size, keyword).stream()
                                .map(p -> mapToResponse(p, identityPort.getUserInfo(p.getUserId())))
                                .collect(Collectors.toList());
                long total = participantRepository.countCheckedInParticipants(meetingId, keyword);
                return PageResponse.of(items, total, page, size);
        }

        @Transactional(readOnly = true)
        public List<AttendanceResponse> searchParticipants(String meetingId, String keyword, int size) {
                return participantRepository.searchParticipants(meetingId, keyword, size).stream()
                                .map(p -> mapToResponse(p, identityPort.getUserInfo(p.getUserId())))
                                .collect(Collectors.toList());
        }

        @Transactional
        public CheckInBundleResponse getCheckInBundle(String meetingId, String cccd) {
                // Kiểm tra sự tồn tại của meeting trước
                if (!meetingPort.canAttend(meetingId)) {
                        throw ParticipantException.invalidState("Cuộc họp không tồn tại hoặc không cho phép điểm danh");
                }

                String userId = identityPort.getUserIdByCccd(cccd)
                                .orElseThrow(() -> ParticipantException.notFound("Không tìm thấy cổ đông"));

                UserDTO user = identityPort.getUserInfo(userId);

                // Tìm participant theo meetingId và userId, nếu không tồn tại thì ném exception
                Participant shareholder = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                                .orElseThrow(() -> ParticipantException
                                                .notFound("Người dùng chưa tham gia cuộc họp này"));

                // Đồng bộ uỷ quyền
                shareholder.setReceivedProxyShares(proxyDelegationRepository.sumReceivedProxyShares(meetingId, userId));
                shareholder.setDelegatedShares(proxyDelegationRepository.sumDelegatedShares(meetingId, userId));
                participantRepository.save(shareholder);

                // Tìm uỷ quyền đi
                List<ProxyDelegation> outgoing = proxyDelegationRepository.findByMeetingIdAndDelegatorId(meetingId,
                                userId, DelegationStatus.ACTIVE);
                List<CheckInBundleResponse.ProxyAttendeeDTO> outgoingDTOs = outgoing.stream()
                                .map(del -> {
                                        UserDTO proxyUser = identityPort.getUserInfo(del.getProxyId());
                                        Participant proxyPart = participantRepository
                                                        .findByMeetingIdAndUserId(meetingId, del.getProxyId())
                                                        .orElseGet(() -> createPendingParticipant(meetingId,
                                                                        proxyUser));
                                        return CheckInBundleResponse.ProxyAttendeeDTO.builder()
                                                        .delegationId(del.getId())
                                                        .sharesDelegated(del.getSharesDelegated())
                                                        .proxyParticipant(mapToResponse(proxyPart, proxyUser))
                                                        .build();
                                }).collect(Collectors.toList());

                // Tìm uỷ quyền đến
                List<ProxyDelegation> incoming = proxyDelegationRepository.findByMeetingIdAndProxyId(meetingId, userId,
                                DelegationStatus.ACTIVE);
                List<CheckInBundleResponse.IncomingProxyDTO> incomingDTOs = incoming.stream()
                                .map(del -> {
                                        UserDTO delegatorUser = identityPort.getUserInfo(del.getDelegatorId());
                                        Participant delegatorPart = participantRepository
                                                        .findByMeetingIdAndUserId(meetingId, del.getDelegatorId())
                                                        .orElseGet(() -> createPendingParticipant(meetingId,
                                                                        delegatorUser));
                                        return CheckInBundleResponse.IncomingProxyDTO.builder()
                                                        .delegationId(del.getId())
                                                        .sharesDelegated(del.getSharesDelegated())
                                                        .delegatorParticipant(
                                                                        mapToResponse(delegatorPart, delegatorUser))
                                                        .delegatorName(delegatorUser.getFullName())
                                                        .delegatorCccd(delegatorUser.getCccd())
                                                        .build();
                                }).collect(Collectors.toList());

                return CheckInBundleResponse.builder()
                                .shareholder(mapToResponse(shareholder, user))
                                .outgoingProxies(outgoingDTOs)
                                .incomingProxies(incomingDTOs)
                                .build();
        }

        @Transactional(readOnly = true)
        public ReconciliationResponse getReconciliation(String meetingId) {
                List<ExpectedAttendance> expectedList = expectedAttendanceRepository.findByMeetingId(meetingId);

                Map<String, ExpectedAttendanceImportRecord> expectedByCccd = expectedList.stream()
                                .collect(Collectors.toMap(ExpectedAttendance::getCccd,
                                                e -> ExpectedAttendanceImportRecord.builder()
                                                                .cccd(e.getCccd())
                                                                .expectedShares(e.getExpectedShares())
                                                                .build(),
                                                (a, b) -> a, LinkedHashMap::new));

                List<ReconciliationItemResponse> items = reconciliationService.buildItems(meetingId, expectedByCccd);

                long totalExpected = items.stream().mapToLong(ReconciliationItemResponse::getExpectedShares).sum();
                long expectedShareholders = items.stream()
                                .filter(i -> i.getExpectedShares() > 0).count();
                long totalSystem = items.stream().mapToLong(ReconciliationItemResponse::getSystemShares).sum();
                long systemShareholders = items.stream()
                                .filter(i -> i.getSystemShares() > 0).count();
                long mismatched = items.stream().filter(i -> "LECH".equals(i.getStatus())).count();

                return ReconciliationResponse.builder()
                                .items(items)
                                .totalExpectedShares(totalExpected)
                                .totalExpectedShareholders(expectedShareholders)
                                .totalSystemShares(totalSystem)
                                .totalSystemShareholders(systemShareholders)
                                .totalMismatched(mismatched)
                                .build();
        }

        @Transactional
        public AttendanceResponse markAsPrinted(String meetingId, String cccd) {
                String userId = identityPort.getUserIdByCccd(cccd)
                                .orElseThrow(() -> ParticipantException.notFound("Cổ đông không tồn tại"));
                Participant p = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                                .orElseThrow(() -> ParticipantException.notFound("Chưa có bản ghi tham dự"));

                p.markAsPrinted();
                return mapToResponse(participantRepository.save(p), identityPort.getUserInfo(userId));
        }

        @Transactional
        public AttendanceResponse cancelAttendance(String meetingId, String cccd) {
                String userId = identityPort.getUserIdByCccd(cccd)
                                .orElseThrow(() -> ParticipantException.notFound("Cổ đông không tồn tại"));

                if (!meetingPort.canAttend(meetingId)) {
                        throw ParticipantException.invalidState(
                                        "Cấu hình hiện tại của cuộc họp không cho phép hủy điểm danh hoặc chưa thiết lập quy tắc.");
                }

                Participant p = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                                .orElseThrow(() -> ParticipantException.notFound("Chưa có bản ghi điểm danh"));

                if (p.getStatus() == ParticipantStatus.PENDING) {
                        throw ParticipantException.invalidState("Cổ đông chưa điểm danh");
                }

                p.setStatus(ParticipantStatus.PENDING);
                p.setAttendingShares(0L);
                p.setCheckedInAt(null);

                return mapToResponse(participantRepository.save(p), identityPort.getUserInfo(userId));
        }

        private Participant createPendingParticipant(String meetingId, UserDTO user) {
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
        }

        private AttendanceResponse mapToResponse(Participant p, UserDTO user) {
                if (user == null) {
                        throw ParticipantException.invalidState(
                                        "Lỗi dữ liệu: Không tìm thấy thông tin người dùng với ID " + p.getUserId());
                }

                return AttendanceResponse.builder()
                                .userId(p.getUserId())
                                .meetingId(p.getMeetingId())
                                .fullName(user.getFullName())
                                .cccd(user.getCccd())
                                .investorCode(user.getInvestorCode())
                                .shareholderCode(user.getInvestorCode())
                                .sharesOwned(p.getSharesOwned())
                                .attendingShares(p.getAttendingShares())
                                .receivedProxyShares(p.getReceivedProxyShares())
                                .delegatedShares(p.getDelegatedShares())
                                .participationType(p.getParticipationType())
                                .status(p.getStatus())
                                .checkedInAt(p.getCheckedInAt())
                                .isRepresentative(user.isSplitAccount())
                                .build();
        }
}
