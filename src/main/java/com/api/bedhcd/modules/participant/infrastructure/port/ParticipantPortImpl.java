package com.api.bedhcd.modules.participant.infrastructure.port;

import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
import com.api.bedhcd.shared.domain.enums.ParticipantStatus;
import com.api.bedhcd.shared.domain.enums.ParticipationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantPortImpl implements ParticipantPort {

    private final ParticipantRepository participantRepository;

    @Override
    public long getVotingPower(String meetingId, String userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(Participant::calculateTotalVotingPower)
                .orElse(0L);
    }

    @Override
    public long getAttendingShares(String meetingId, String userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(Participant::getAttendingShares)
                .orElse(0L);
    }

    @Override
    public long getReceivedProxyShares(String meetingId, String userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(Participant::getReceivedProxyShares)
                .orElse(0L);
    }

    @Override
    public long getDelegatedShares(String meetingId, String userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(Participant::getDelegatedShares)
                .orElse(0L);
    }

    @Override
    public boolean isCheckedIn(String meetingId, String userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(Participant::isCheckedIn)
                .orElse(false);
    }

    @Override
    public boolean isPrinted(String meetingId, String userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(p -> p.getStatus() == ParticipantStatus.PRINT)
                .orElse(false);
    }

    @Override
    public java.time.LocalDateTime getCheckedInAt(String meetingId, String userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(Participant::getCheckedInAt)
                .orElse(null);
    }

    @Override
    public long countTotalParticipants() {
        return participantRepository.count();
    }

    @Override
    public long countTotalCheckedIn() {
        return participantRepository.countByCheckedIn(true);
    }

    @Override
    public long sumTotalShares() {
        return participantRepository.sumTotalShares();
    }

    @Override
    public long sumCheckedInShares() {
        return participantRepository.sumCheckedInShares();
    }

    @Override
    public long countByMeetingId(String meetingId) {
        return participantRepository.countByMeetingId(meetingId);
    }

    @Override
    public long countCheckedInByMeetingId(String meetingId) {
        return participantRepository.countCheckedInByMeetingId(meetingId);
    }

    @Override
    public long sumTotalSharesByMeetingId(String meetingId) {
        return participantRepository.sumTotalSharesByMeetingId(meetingId);
    }

    @Override
    public long sumCheckedInSharesByMeetingId(String meetingId) {
        return participantRepository.sumCheckedInSharesByMeetingId(meetingId);
    }

    @Override
    public String getLastMeetingId(String userId) {
        return participantRepository.findByUserId(userId).stream()
                .map(Participant::getMeetingId)
                .findFirst() // Tạm lấy cái đầu tiên tìm thấy
                .orElse(null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void createParticipant(String meetingId, String userId, Long sharesOwned) {
        Participant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseGet(() -> Participant.builder()
                        .meetingId(meetingId)
                        .userId(userId)
                        .build());
        
        participant.setSharesOwned(sharesOwned != null ? sharesOwned : 0L);
        participant.setStatus(ParticipantStatus.PENDING);
        participant.setParticipationType(ParticipationType.DIRECT);
        if (participant.getAttendingShares() == null) participant.setAttendingShares(0L);
        if (participant.getReceivedProxyShares() == null) participant.setReceivedProxyShares(0L);
        if (participant.getDelegatedShares() == null) participant.setDelegatedShares(0L);

        participantRepository.save(participant);
    }

    @Override
    public java.util.List<String> getParticipantUserIds(String meetingId) {
        return participantRepository.findByMeetingId(meetingId).stream()
                .map(Participant::getUserId)
                .collect(java.util.stream.Collectors.toList());
    }
}

