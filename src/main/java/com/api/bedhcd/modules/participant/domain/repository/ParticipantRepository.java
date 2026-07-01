package com.api.bedhcd.modules.participant.domain.repository;

import com.api.bedhcd.modules.participant.domain.model.Participant;
import java.util.Optional;

public interface ParticipantRepository {
    Optional<Participant> findByMeetingIdAndUserId(String meetingId, String userId);
    java.util.List<Participant> findAllByMeetingIdAndUserIdIn(String meetingId, java.util.List<String> userIds);
    java.util.List<Participant> findByUserId(String userId);
    java.util.List<Participant> findByMeetingId(String meetingId);
    java.util.List<Participant> findCheckedInParticipants(String meetingId);
    Participant save(Participant participant);
    java.util.List<Participant> saveAll(java.util.List<Participant> participants);
    long count();
    long countByCheckedIn(boolean checkedIn);
    long countByMeetingId(String meetingId);
    long countCheckedInByMeetingId(String meetingId);
    long sumTotalShares();
    long sumCheckedInShares();
    long sumTotalSharesByMeetingId(String meetingId);
    long sumCheckedInSharesByMeetingId(String meetingId);
}
