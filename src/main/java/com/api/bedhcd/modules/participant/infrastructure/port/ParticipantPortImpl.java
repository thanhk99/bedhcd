package com.api.bedhcd.modules.participant.infrastructure.port;

import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.participant.domain.model.Participant;
import com.api.bedhcd.modules.participant.domain.repository.ParticipantRepository;
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
}
