package com.api.bedhcd.modules.meeting.application.service;

import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingResponse;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingRealtimeResponse;
import com.api.bedhcd.modules.meeting.application.mapper.MeetingMapper;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.meeting.domain.exception.MeetingException;
import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingConfigRepository;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingRepository;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.resolution.application.port.ResolutionPort;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingApplicationService {

    private final MeetingRepository meetingRepository;
    private final MeetingConfigRepository configRepository;
    private final MeetingMapper meetingMapper;
    private final MeetingPort meetingPort;
    private final ParticipantPort participantPort;
    private final ResolutionPort resolutionPort;
    private final VotingPort votingPort;

    @Transactional(readOnly = true)
    public List<MeetingResponse> getAll() {
        List<Meeting> meetings = meetingRepository.findAll();
        Map<String, MeetingConfig> configCache = loadConfigs(meetings);
        return meetings.stream()
                .map(m -> meetingMapper.toResponse(m, configCache.get(m.getConfigId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingResponse getById(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));
        MeetingConfig config = loadConfig(meeting.getConfigId());
        return meetingMapper.toResponse(meeting, config);
    }

    @Transactional(readOnly = true)
    public MeetingResponse getOngoingMeeting() {
        return meetingRepository.findOngoing()
                .map(m -> {
                    MeetingConfig config = loadConfig(m.getConfigId());
                    return meetingMapper.toResponse(m, config);
                })
                .orElse(null);
    }

    private MeetingConfig loadConfig(String configId) {
        if (configId == null || configId.isEmpty())
            return null;
        return configRepository.findById(configId).orElse(null);
    }

    private Map<String, MeetingConfig> loadConfigs(List<Meeting> meetings) {
        Set<String> configIds = meetings.stream()
                .map(Meeting::getConfigId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        if (configIds.isEmpty())
            return Map.of();
        return configRepository.findAll().stream()
                .filter(c -> configIds.contains(c.getId()))
                .collect(Collectors.toMap(MeetingConfig::getId, c -> c));
    }

    @Transactional(readOnly = true)
    public com.api.bedhcd.modules.meeting.api.v1.dto.MeetingRealtimeResponse getRealtimeStats(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        long totalParticipants = participantPort.countByMeetingId(id);
        long checkedInCount = participantPort.countCheckedInByMeetingId(id);
        long totalShares = participantPort.sumTotalSharesByMeetingId(id);
        long checkedInShares = participantPort.sumCheckedInSharesByMeetingId(id);

        double participationRate = totalShares > 0
                ? (double) checkedInShares * 100 / totalShares
                : 0;

        long totalResolutions = resolutionPort.countResolutionsByMeetingId(id);
        long totalVotes = votingPort.countVotesByMeetingId(id);

        return MeetingRealtimeResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .status(meeting.getStatus())
                .attendance(MeetingRealtimeResponse.AttendanceStats.builder()
                        .totalParticipants(totalParticipants)
                        .checkedInCount(checkedInCount)
                        .totalShares(totalShares)
                        .checkedInShares(checkedInShares)
                        .participationRate(participationRate)
                        .build())
                .voting(MeetingRealtimeResponse.VotingStats.builder()
                        .totalResolutions(totalResolutions)
                        .totalVotes(totalVotes)
                        .build())
                .build();
    }

    @Transactional
    public MeetingResponse createMeeting(Meeting meeting) {
        if (meeting.getStatus() == null || meeting.getStatus().isEmpty()) {
            meeting.setStatus(MeetingStatus.SCHEDULED);
        }
        Meeting saved = meetingRepository.save(meeting);
        MeetingConfig config = loadConfig(saved.getConfigId());
        return meetingMapper.toResponse(saved, config);
    }

    @Transactional
    public MeetingResponse updateMeeting(String id, Meeting updateInfo) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        meeting.setTitle(updateInfo.getTitle());
        meeting.setDescription(updateInfo.getDescription());
        meeting.setStartTime(updateInfo.getStartTime());
        meeting.setEndTime(updateInfo.getEndTime());
        meeting.setLocation(updateInfo.getLocation());
        meeting.setConfigId(updateInfo.getConfigId());
        meeting.setStatus(updateInfo.getStatus());

        Meeting saved = meetingRepository.save(meeting);
        MeetingConfig config = loadConfig(saved.getConfigId());
        return meetingMapper.toResponse(saved, config);
    }

    @Transactional
    public MeetingResponse updateStatus(String id, String status) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        meeting.setStatus(status);
        meeting.setUpdatedAt(java.time.LocalDateTime.now());

        Meeting saved = meetingRepository.save(meeting);
        MeetingConfig config = loadConfig(saved.getConfigId());
        return meetingMapper.toResponse(saved, config);
    }

    @Transactional
    public void deleteMeeting(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        if (!meetingPort.canEditMeeting(id)) {
            throw MeetingException.invalidState(
                    "Trạng thái hiện tại không cho phép xóa cuộc họp.");
        }

        if (!meeting.canDelete()) {
            throw MeetingException.invalidState("Chỉ có thể xóa cuộc họp ở trạng thái Sắp diễn ra.");
        }

        meetingRepository.deleteById(id);
    }
}
