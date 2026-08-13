package com.api.bedhcd.modules.participant.application.service;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.model.ProxyDelegation;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.modules.participant.domain.repository.ProxyDelegationRepository;
import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import com.api.bedhcd.shared.domain.enums.Role;
import com.api.bedhcd.shared.dto.UserDTO;
import com.api.bedhcd.shared.dto.importing.ProxyImportRecord;
import com.api.bedhcd.shared.dto.importing.ShareholderImportRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service riêng để xử lý batch import với @Transactional thực sự hiệu quả.
 * Tách ra khỏi AsyncImportService để tránh self-invocation khiến Spring AOP không intercept được.
 */
@Service
@RequiredArgsConstructor
public class ImportBatchService {

    private final IdentityPort identityPort;
    private final ParticipantRepository participantRepository;
    private final ProxyDelegationRepository proxyDelegationRepository;

    @Transactional
    public void processShareholderBatch(String meetingId, List<ShareholderImportRecord> batch) {
        // 1. Build DTOs
        List<UserDTO> userDTOs = batch.stream().map(record -> UserDTO.builder()
                .cccd(record.getCccd())
                .fullName(record.getFullName())
                .email(record.getEmail())
                .investorCode(record.getInvestorCode())
                .sharesOwned(record.getShares())
                .phoneNumber(record.getPhoneNumber())
                .enabled(true)
                .build()).collect(Collectors.toList());

        // 2. Batch upsert users (1 query findAllByCccdIn + 1 saveAll)
        List<UserDTO> savedUsers = identityPort.createOrUpdateUserBatch(userDTOs);
        Map<String, UserDTO> userMap = savedUsers.stream()
                .collect(Collectors.toMap(UserDTO::getCccd, u -> u, (u1, u2) -> u1));

        // 3. Batch load existing participants (1 query thay vì N queries)
        List<String> userIds = savedUsers.stream().map(UserDTO::getId).collect(Collectors.toList());
        Map<String, Participant> existingParticipantMap = participantRepository
                .findAllByMeetingIdAndUserIdIn(meetingId, userIds)
                .stream()
                .collect(Collectors.toMap(Participant::getUserId, p -> p, (p1, p2) -> p1));

        // 4. Batch upsert participants - không còn N+1 query
        List<Participant> participantsToSave = batch.stream().map(record -> {
            UserDTO user = userMap.get(record.getCccd());
            if (user == null) return null;

            Participant participant = existingParticipantMap.getOrDefault(
                    user.getId(),
                    Participant.builder()
                            .meetingId(meetingId)
                            .userId(user.getId())
                            .build()
            );

            participant.setSharesOwned(user.getSharesOwned());
            participant.setStatus(ParticipantStatus.PENDING);
            participant.setParticipationType(ParticipationType.DIRECT);
            if (participant.getAttendingShares() == null) participant.setAttendingShares(0L);
            if (participant.getReceivedProxyShares() == null) participant.setReceivedProxyShares(0L);
            if (participant.getDelegatedShares() == null) participant.setDelegatedShares(0L);

            return participant;
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        participantRepository.saveAll(participantsToSave);
    }

    @Transactional
    public void processProxyBatch(String meetingId, List<ProxyImportRecord> batch) {
        for (ProxyImportRecord record : batch) {
            try {
                String delegatorId = identityPort.getUserIdByCccd(record.getDelegatorCccd())
                        .orElseThrow(() -> new RuntimeException(
                                "Không tìm thấy cổ đông ủy quyền với CCCD: " + record.getDelegatorCccd()));

                String proxyId = identityPort.getUserIdByCccd(record.getProxyCccd())
                        .orElseGet(() -> identityPort.createOrUpdateUser(UserDTO.builder()
                                .cccd(record.getProxyCccd())
                                .fullName(record.getFullName())
                                .roles(Set.of(Role.REPRESENTATIVE))
                                .splitAccount(true)
                                .build()).getId());

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
                throw new RuntimeException(
                        "Lỗi dòng ủy quyền CCCD: " + record.getDelegatorCccd() + " - " + e.getMessage());
            }
        }
    }
}
