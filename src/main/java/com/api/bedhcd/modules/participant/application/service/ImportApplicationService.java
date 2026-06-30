package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.participant.domain.exception.ParticipantException;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.modules.participant.domain.repository.ProxyDelegationRepository;
import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import com.api.bedhcd.shared.dto.UserDTO;
import com.api.bedhcd.shared.dto.importing.ProxyImportRecord;
import com.api.bedhcd.shared.dto.importing.ShareholderImportRecord;
import com.api.bedhcd.util.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportApplicationService {

    private final IdentityPort identityPort;
    private final ParticipantRepository participantRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;
    private final MeetingPort meetingPort;

    @Transactional
    public void importShareholders(String meetingId, MultipartFile file) {
        if (!meetingPort.canImportShareholder(meetingId)) {
            throw ParticipantException.invalidState(
                    "Trạng thái hiện tại của cuộc họp không cho phép import cổ đông.");
        }

        List<ShareholderImportRecord> records = ExcelHelper.parseShareholders(file);

        for (ShareholderImportRecord record : records) {
            // 1. Tạo hoặc cập nhật User
            UserDTO user = identityPort.createOrUpdateUser(UserDTO.builder()
                    .cccd(record.getCccd())
                    .fullName(record.getFullName())
                    .email(record.getEmail())
                    .investorCode(record.getInvestorCode())
                    .sharesOwned(record.getShares())
                    .phoneNumber(record.getPhoneNumber())
                    .enabled(true)
                    .build());

            // 2. Tạo hoặc cập nhật Participant
            Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, user.getId())
                    .orElse(Participant.builder()
                            .meetingId(meetingId)
                            .userId(user.getId())
                            .build());

            participant.setSharesOwned(user.getSharesOwned());
            participant.setStatus(ParticipantStatus.PENDING);
            participant.setParticipationType(ParticipationType.DIRECT);
            participant.setAttendingShares(0L);
            participant.setReceivedProxyShares(0L);
            participant.setDelegatedShares(0L);

            participantRepository.save(participant);
        }
    }

    @Transactional
    public void importProxies(String meetingId, MultipartFile file) {
        if (!meetingPort.canRegisterProxy(meetingId)) {
            throw ParticipantException.invalidState(
                    "Trạng thái hiện tại của cuộc họp không cho phép import ủy quyền.");
        }

        List<ProxyImportRecord> records = ExcelHelper.parseProxies(file);

        for (ProxyImportRecord record : records) {
            try {
                // Tìm delegator
                String delegatorId = identityPort.getUserIdByCccd(record.getDelegatorCccd())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy cổ đông ủy quyền với CCCD: " + record.getDelegatorCccd()));

                // Tìm hoặc tạo proxy
                String proxyId = identityPort.getUserIdByCccd(record.getProxyCccd())
                        .orElseGet(() -> identityPort.createOrUpdateUser(UserDTO.builder()
                                .cccd(record.getProxyCccd())
                                .fullName(record.getFullName())
                                .build()).getId());

                // Tạo ủy quyền
                ProxyDelegation delegation = ProxyDelegation.builder()
                        .meetingId(meetingId)
                        .delegatorId(delegatorId)
                        .proxyId(proxyId)
                        .sharesDelegated(record.getSharesDelegated())
                        .status(DelegationStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build();

                proxyDelegationRepository.save(delegation);
            } catch (Exception e) {
                System.err.println("Lỗi khi import dòng ủy quyền: " + e.getMessage());
            }
        }
    }
}
