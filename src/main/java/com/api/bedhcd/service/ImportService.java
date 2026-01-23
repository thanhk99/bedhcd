package com.api.bedhcd.service;

import com.api.bedhcd.dto.importing.ProxyImportRecord;
import com.api.bedhcd.dto.importing.ShareholderImportRecord;
import com.api.bedhcd.entity.*;
import com.api.bedhcd.entity.enums.DelegationStatus;
import com.api.bedhcd.entity.enums.ParticipantStatus;
import com.api.bedhcd.entity.enums.ParticipationType;
import com.api.bedhcd.exception.BadRequestException;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.*;
import com.api.bedhcd.util.ExcelHelper;
import com.api.bedhcd.util.RandomUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final ProxyDelegationRepository proxyRepository;
    private final ImportLogRepository logRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void importShareholders(String meetingId, MultipartFile file) {
        log.info("Start importing shareholders for meetingId={}, file={}", meetingId, file.getOriginalFilename());

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> {
                    log.error("Meeting not found: {}", meetingId);
                    return new ResourceNotFoundException("Meeting not found");
                });

        List<ShareholderImportRecord> records = ExcelHelper.parseShareholders(file);
        log.info("Parsed {} shareholder records", records.size());

        for (ShareholderImportRecord record : records) {
            try {
                log.info("Processing shareholder CCCD={}", record.getCccd());

                User user = userRepository.findByCccd(record.getCccd())
                        .orElseGet(() -> {
                            log.info("Creating new user for CCCD={}", record.getCccd());
                            Set<Role> roles = new HashSet<>();
                            roles.add(Role.SHAREHOLDER);
                            User newUser = User.builder()
                                    .id(RandomUtil.generate6DigitId(userRepository::existsById))
                                    .cccd(record.getCccd())
                                    .fullName(record.getFullName())
                                    .email(record.getEmail() != null
                                            ? record.getEmail()
                                            : record.getCccd() + "@bedhcd.com")
                                    .phoneNumber(record.getPhoneNumber() != null
                                            ? record.getPhoneNumber()
                                            : "0000000000")
                                    .address(record.getAddress() != null
                                            ? record.getAddress()
                                            : "N/A")
                                    .investorCode(record.getInvestorCode())
                                    .dateOfIssue(record.getDateOfIssue() != null
                                            ? record.getDateOfIssue()
                                            : "N/A")
                                    .placeOfIssue(record.getPlaceOfIssue())
                                    .nation(record.getNation() != null
                                            ? record.getNation()
                                            : "Việt Nam")
                                    .password(passwordEncoder.encode(
                                            record.getPassword() != null && !record.getPassword().isEmpty()
                                                    ? record.getPassword()
                                                    : record.getCccd()))
                                    .roles(roles)
                                    .enabled(true)
                                    .sharesOwned(0L)
                                    .build();
                            return userRepository.save(newUser);
                        });

                // update user
                user.setFullName(record.getFullName());
                if (record.getInvestorCode() != null)
                    user.setInvestorCode(record.getInvestorCode());
                if (record.getDateOfIssue() != null)
                    user.setDateOfIssue(record.getDateOfIssue());
                if (record.getPlaceOfIssue() != null)
                    user.setPlaceOfIssue(record.getPlaceOfIssue());
                if (record.getNation() != null)
                    user.setNation(record.getNation());

                userRepository.save(user);

                MeetingParticipant participant = participantRepository
                        .findByMeeting_IdAndUser_Id(meetingId, user.getId())
                        .orElse(MeetingParticipant.builder()
                                .meeting(meeting)
                                .user(user)
                                .participationType(ParticipationType.DIRECT)
                                .status(ParticipantStatus.PENDING)
                                .build());

                participant.setTotalShares(record.getShares());
                participant.setSharesOwned(record.getShares());
                participant.setDelegatedShares(0L);
                participant.setReceivedProxyShares(0L);

                participantRepository.save(participant);

            } catch (Exception e) {
                log.error(
                        "Error importing shareholder CCCD={}, meetingId={}",
                        record.getCccd(),
                        meetingId,
                        e);
                throw e; // để rollback transaction
            }
        }

        saveLog(meetingId, "SHAREHOLDERS", file.getOriginalFilename(), records.size());
        log.info("Finished importing shareholders for meetingId={}", meetingId);
    }

    @Transactional
    public void importProxies(String meetingId, MultipartFile file) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        List<ProxyImportRecord> records = ExcelHelper.parseProxies(file);

        for (ProxyImportRecord record : records) {
            User delegatorUser = userRepository.findByCccd(record.getDelegatorCccd())
                    .orElseThrow(() -> new BadRequestException("Delegator not found: " + record.getDelegatorCccd()));
            User proxyUser = userRepository.findByCccd(record.getProxyCccd())
                    .orElseThrow(() -> new BadRequestException("Proxy not found: " + record.getProxyCccd()));

            MeetingParticipant delegator = participantRepository
                    .findByMeeting_IdAndUser_Id(meetingId, delegatorUser.getId())
                    .orElseThrow(
                            () -> new BadRequestException("Delegator not in meeting: " + record.getDelegatorCccd()));
            MeetingParticipant proxy = participantRepository.findByMeeting_IdAndUser_Id(meetingId, proxyUser.getId())
                    .orElseThrow(() -> new BadRequestException("Proxy not in meeting: " + record.getProxyCccd()));

            if (record.getSharesDelegated() > delegator.getSharesOwned()) {
                throw new BadRequestException("Not enough shares for delegation by " + record.getDelegatorCccd());
            }

            // Create Proxy Delegation record
            ProxyDelegation delegation = ProxyDelegation.builder()
                    .meeting(meeting)
                    .delegator(delegatorUser)
                    .proxy(proxyUser)
                    .sharesDelegated(record.getSharesDelegated())
                    .authorizationDocument(record.getAuthorizationDocument())
                    .authorizationDate(record.getAuthorizationDate())
                    .description(record.getDescription())
                    .status(DelegationStatus.ACTIVE)
                    .build();
            proxyRepository.save(delegation);

            // Update participant counts
            delegator.setSharesOwned(delegator.getSharesOwned() - record.getSharesDelegated());
            delegator.setDelegatedShares(delegator.getDelegatedShares() + record.getSharesDelegated());
            proxy.setReceivedProxyShares(proxy.getReceivedProxyShares() + record.getSharesDelegated());

            participantRepository.save(delegator);
            participantRepository.save(proxy);
        }

        saveLog(meetingId, "PROXIES", file.getOriginalFilename(), records.size());
    }

    private void saveLog(String meetingId, String type, String fileName, int count) {
        ImportLog log = ImportLog.builder()
                .meetingId(meetingId)
                .importType(type)
                .fileName(fileName)
                .totalRecords(count)
                .build();
        logRepository.save(log);
    }
}
