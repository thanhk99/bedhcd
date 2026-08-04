package com.api.bedhcd.modules.participant.domain.repository;

import java.util.List;
import java.util.Optional;

import com.api.bedhcd.modules.participant.domain.model.Participant;

public interface ParticipantRepository {
    Optional<Participant> findByMeetingIdAndUserId(String meetingId, String userId);

    List<Participant> findAllByMeetingIdAndUserIdIn(String meetingId, List<String> userIds);

    List<Participant> findByUserId(String userId);

    List<Participant> findByMeetingId(String meetingId);

    List<Participant> findCheckedInParticipants(String meetingId);

    List<Participant> findCheckedInParticipants(String meetingId, int page, int size, String keyword);

    long countCheckedInParticipants(String meetingId, String keyword);

    Participant save(Participant participant);

    List<Participant> saveAll(List<Participant> participants);

    long count();

    long countByCheckedIn(boolean checkedIn);

    long countByMeetingId(String meetingId);

    long countCheckedInByMeetingId(String meetingId);

    long sumTotalShares();

    long sumCheckedInShares();

    long sumTotalSharesByMeetingId(String meetingId);

    long sumCheckedInSharesByMeetingId(String meetingId);
}

