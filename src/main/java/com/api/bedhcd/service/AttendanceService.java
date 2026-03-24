package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.AttendanceRequest;
import com.api.bedhcd.dto.response.AttendanceResponse;
import com.api.bedhcd.entity.Meeting;
import com.api.bedhcd.entity.MeetingParticipant;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.entity.enums.ParticipantStatus;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.dto.response.CheckInBundleResponse;
import com.api.bedhcd.entity.ProxyDelegation;
import com.api.bedhcd.entity.enums.DelegationStatus;
import com.api.bedhcd.repository.MeetingParticipantRepository;
import com.api.bedhcd.repository.MeetingRepository;
import com.api.bedhcd.repository.ProxyDelegationRepository;
import com.api.bedhcd.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

        private final MeetingParticipantRepository participantRepository;
        private final MeetingRepository meetingRepository;
        private final UserRepository userRepository;
        private final ProxyDelegationRepository proxyDelegationRepository;

        @Transactional
        public AttendanceResponse registerAttendance(AttendanceRequest request) {
                log.info("Registering attendance for investorCode: {} in meeting: {}",
                                request.getInvestorCode(), request.getMeetingId());

                Meeting meeting = meetingRepository.findById(request.getMeetingId())
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                User user = userRepository.findByInvestorCode(request.getInvestorCode())
                                .or(() -> userRepository.findByCccd(request.getInvestorCode()))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Shareholder not found with code: " + request.getInvestorCode()));

                MeetingParticipant participant = participantRepository
                                .findByMeeting_IdAndUser_Id(request.getMeetingId(), user.getId())
                                .orElse(MeetingParticipant.builder()
                                                .meeting(meeting)
                                                .user(user)
                                                .build());

                // Cập nhật thông tin sở hữu từ User sang Participant tại thời điểm tham dự
                participant.setSharesOwned(user.getSharesOwned() != null ? user.getSharesOwned() : 0L);

                Long newAttendingShares = request.getAttendingShares() != null ? request.getAttendingShares() : 0L;
                Long oldAttendingShares = participant.getAttendingShares() != null ? participant.getAttendingShares()
                                : 0L;
                // Lấy số liệu thực tế từ bảng ủy quyền để đảm bảo chính xác
                Long receivedProxyShares = proxyDelegationRepository.sumReceivedProxyShares(request.getMeetingId(), user.getId());
                Long delegatedShares = participant.getDelegatedShares() != null ? participant.getDelegatedShares() : 0L;

                // Validate
                long maxAvailableShares = user.getSharesOwned() - delegatedShares + receivedProxyShares;

                if (newAttendingShares < 0) {
                        throw new BadRequestException("Attending shares cannot be negative");
                }
                if (newAttendingShares > maxAvailableShares) {
                        throw new BadRequestException("Attending shares exceed available shares. Max available: "
                                        + maxAvailableShares);
                }

                // ─ Xử lý cập nhật phiếu uỷ quyền ─
                // Nếu participant đã check-in và đây là lần cập nhật (delta != 0)
                if (participant.getStatus() == ParticipantStatus.CHECKED_IN
                                && !oldAttendingShares.equals(newAttendingShares)) {
                        long delta = newAttendingShares - oldAttendingShares; // dương = proxy lấy thêm, âm = proxy trả
                                                                              // lại

                        // Tìm delegation record để biết delegator là ai
                        List<ProxyDelegation> delegations = proxyDelegationRepository
                                        .findByMeeting_IdAndProxy_IdAndStatus(request.getMeetingId(), user.getId(),
                                                        DelegationStatus.ACTIVE);

                        if (!delegations.isEmpty()) {
                                ProxyDelegation delegation = delegations.get(0);
                                participantRepository
                                                .findByMeeting_IdAndUser_Id(request.getMeetingId(),
                                                                delegation.getDelegator().getId())
                                                .ifPresent(delegatorPart -> {
                                                        // Delegator nhận lại (hoặc cho đi thêm) phần chênh lệch
                                                        long delegatorAttending = delegatorPart
                                                                        .getAttendingShares() != null
                                                                                        ? delegatorPart.getAttendingShares()
                                                                                        : 0L;
                                                        long delegatorReceived = delegatorPart
                                                                        .getReceivedProxyShares() != null
                                                                                        ? delegatorPart.getReceivedProxyShares()
                                                                                        : 0L;
                                                        long newDelegatorAttending = Math.max(0,
                                                                        delegatorAttending - delta);
                                                        delegatorPart.setAttendingShares(newDelegatorAttending);
                                                        delegatorPart.setAttendingShares(newDelegatorAttending);
                                                        log.info("Adjusting delegator {} attending shares: {} -> {} (delta: {})",
                                                                        delegation.getDelegator().getId(),
                                                                        delegatorAttending, newDelegatorAttending,
                                                                        delta);
                                                        participantRepository.save(delegatorPart);
                                                });
                        }
                }

                participant.setAttendingShares(newAttendingShares);

                // Loại hình tham gia lấy từ request (tránh tự thay đổi sang Proxy nếu là Cổ
                // đông đi dự trực tiếp)
                participant.setParticipationType(request.getParticipationType());

                participant.setStatus(ParticipantStatus.CHECKED_IN);
                participant.setCheckedInAt(LocalDateTime.now());

                // Quyền biểu quyết = Số tham dự thực tế (đã tính gộp uỷ quyền nếu có)

                participant = participantRepository.save(participant);
                return mapToResponse(participant);
        }

        @Transactional
        public AttendanceResponse cancelAttendance(String meetingId, String investorCode) {
                log.info("Canceling attendance for investorCode: {} in meeting: {}",
                                investorCode, meetingId);

                User user = userRepository.findByInvestorCode(investorCode)
                                .or(() -> userRepository.findByCccd(investorCode))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Shareholder not found with code: " + investorCode));

                MeetingParticipant participant = participantRepository
                                .findByMeeting_IdAndUser_Id(meetingId, user.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));

                if (participant.getStatus() != ParticipantStatus.CHECKED_IN) {
                        throw new BadRequestException("Shareholder is not checked in");
                }

                participant.setAttendingShares(0L);
                participant.setStatus(ParticipantStatus.PENDING);
                participant.setCheckedInAt(null);

                participant = participantRepository.save(participant);
                return mapToResponse(participant);
        }

        @Transactional(readOnly = true)
        public List<AttendanceResponse> getAttendedParticipants(String meetingId) {
                return participantRepository.findByMeeting_Id(meetingId).stream()
                                .filter(p -> p.getStatus() == ParticipantStatus.CHECKED_IN)
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public CheckInBundleResponse getCheckInBundle(String meetingId, String keyword) {
                Meeting meeting = meetingRepository.findById(meetingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

                // Tìm User theo investorCode hoặc CCCD
                User user = userRepository.findByInvestorCode(keyword)
                                .or(() -> userRepository.findByCccd(keyword))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy cổ đông với mã: " + keyword));

                // Lấy hoặc tạo Participant cho cổ đông chính
                MeetingParticipant shareholderPart = participantRepository
                                .findByMeeting_IdAndUser_Id(meetingId, user.getId())
                                .orElseGet(() -> createPendingParticipant(meeting, user));

                // Đồng bộ sharesOwned từ User gốc
                shareholderPart.setSharesOwned(user.getSharesOwned());
                shareholderPart = participantRepository.save(shareholderPart);

                AttendanceResponse shareholderResponse = mapToResponse(shareholderPart);

                // Tìm các uỷ quyền ACTIVE mà cổ đông này là người uỷ quyền (Uỷ quyền đi)
                List<ProxyDelegation> outgoingDels = proxyDelegationRepository
                                .findByMeeting_IdAndDelegator_IdAndStatus(meetingId, user.getId(),
                                                DelegationStatus.ACTIVE);

                List<CheckInBundleResponse.ProxyAttendeeDTO> outgoingDTOs = outgoingDels.stream()
                                .map(del -> {
                                        User proxyUser = del.getProxy();
                                        MeetingParticipant proxyPart = participantRepository
                                                        .findByMeeting_IdAndUser_Id(meetingId, proxyUser.getId())
                                                        .orElseGet(() -> createPendingParticipant(meeting, proxyUser));

                                        proxyPart.setSharesOwned(proxyUser.getSharesOwned());
                                        proxyPart = participantRepository.save(proxyPart);

                                        return CheckInBundleResponse.ProxyAttendeeDTO.builder()
                                                        .delegationId(del.getId())
                                                        .sharesDelegated(del.getSharesDelegated())
                                                        .proxyParticipant(mapToResponse(proxyPart))
                                                        .build();
                                })
                                .collect(Collectors.toList());

                // Tìm các uỷ quyền ACTIVE mà cổ đông này là người nhận uỷ quyền (Uỷ quyền đến)
                List<ProxyDelegation> incomingDels = proxyDelegationRepository
                                .findByMeeting_IdAndProxy_IdAndStatus(meetingId, user.getId(),
                                                DelegationStatus.ACTIVE);

                List<CheckInBundleResponse.IncomingProxyDTO> incomingDTOs = incomingDels.stream()
                                .map(del -> {
                                        User delegatorUser = del.getDelegator();
                                        MeetingParticipant delegatorPart = participantRepository
                                                        .findByMeeting_IdAndUser_Id(meetingId, delegatorUser.getId())
                                                        .orElseGet(() -> createPendingParticipant(meeting,
                                                                        delegatorUser));

                                        delegatorPart.setSharesOwned(delegatorUser.getSharesOwned());
                                        delegatorPart = participantRepository.save(delegatorPart);

                                        return CheckInBundleResponse.IncomingProxyDTO.builder()
                                                        .delegationId(del.getId())
                                                        .sharesDelegated(del.getSharesDelegated())
                                                        .delegatorParticipant(mapToResponse(delegatorPart))
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return CheckInBundleResponse.builder()
                                .shareholder(shareholderResponse)
                                .outgoingProxies(outgoingDTOs)
                                .incomingProxies(incomingDTOs)
                                .build();
        }

        private MeetingParticipant createPendingParticipant(Meeting meeting, User user) {
                MeetingParticipant p = MeetingParticipant.builder()
                                .meeting(meeting)
                                .user(user)
                                .sharesOwned(user.getSharesOwned() != null ? user.getSharesOwned() : 0L)
                                .status(ParticipantStatus.PENDING)
                                .participationType(com.api.bedhcd.entity.enums.ParticipationType.DIRECT)
                                .attendingShares(0L)
                                .receivedProxyShares(0L)
                                .delegatedShares(0L)
                                .delegatedShares(0L)
                                .build();
                return participantRepository.save(p);
        }

        private AttendanceResponse mapToResponse(MeetingParticipant p) {
                String investorCode = p.getUser().getInvestorCode();
                if (investorCode == null || investorCode.isBlank()) {
                        investorCode = p.getUser().getCccd();
                }

                return AttendanceResponse.builder()
                                .userId(p.getUser().getId())
                                .meetingId(p.getMeeting().getId())
                                .investorCode(investorCode)
                                .shareholderCode(investorCode)
                                .fullName(p.getUser().getFullName())
                                .cccd(p.getUser().getCccd())
                                .dateOfIssue(p.getUser().getDateOfIssue())
                                .placeOfIssue(p.getUser().getPlaceOfIssue())
                                .phoneNumber(p.getUser().getPhoneNumber())
                                .email(p.getUser().getEmail())
                                .sharesOwned(p.getSharesOwned() != null ? p.getSharesOwned() : 0L)
                                .attendingShares(p.getAttendingShares() != null ? p.getAttendingShares() : 0L)
                                .receivedProxyShares(
                                                p.getReceivedProxyShares() != null ? p.getReceivedProxyShares() : 0L)
                                .delegatedShares(p.getDelegatedShares() != null ? p.getDelegatedShares() : 0L)
                                .participationType(p.getParticipationType())
                                .checkedInAt(p.getCheckedInAt())
                                .checkedInBy(p.getCreatedBy())
                                .checkedInByName(getUserNameById(p.getCreatedBy()))
                                .build();
        }

        private String getUserNameById(String id) {
                if (id == null || id.equals("SYSTEM"))
                        return "Hệ thống";
                return userRepository.findById(id)
                                .map(User::getFullName)
                                .orElse(id);
        }
}
