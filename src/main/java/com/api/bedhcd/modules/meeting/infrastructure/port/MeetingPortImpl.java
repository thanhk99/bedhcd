package com.api.bedhcd.modules.meeting.infrastructure.port;

import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import com.api.bedhcd.modules.meeting.domain.model.MeetingRules;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingConfigRepository;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class MeetingPortImpl implements MeetingPort {

    private final MeetingRepository meetingRepository;
    private final MeetingConfigRepository configRepository;

    @Override
    public String getStatus(String meetingId) {
        return meetingRepository.findById(meetingId)
                .map(Meeting::getStatus)
                .orElse(null);
    }

    @Override
    public MeetingRules getRules(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        if (meeting == null) {
            log.warn("getRules: Meeting not found for id {}", meetingId);
            return null;
        }

        log.info("getRules: Meeting status is {}, configId is {}", meeting.getStatus(), meeting.getConfigId());

        if (meeting.getConfigId() == null || meeting.getConfigId().isEmpty()) {
            log.warn("getRules: Meeting configId is null or empty");
            return null;
        }

        MeetingConfig config = configRepository.findById(meeting.getConfigId()).orElse(null);
        if (config == null) {
            log.warn("getRules: MeetingConfig not found for id {}", meeting.getConfigId());
            return null;
        }

        log.info("getRules: Found config with keys {}",
                config.getStateConfigs() != null ? config.getStateConfigs().keySet() : "null");

        MeetingRules rules = config.getRulesForStatus(meeting.getStatus());
        if (rules == null) {
            log.warn("getRules: No rules found in config for status {}", meeting.getStatus());
        }
        return rules;
    }

    @Override
    public String getConfigId(String meetingId) {
        return meetingRepository.findById(meetingId)
                .map(Meeting::getConfigId)
                .orElse(null);
    }

    @Override
    public long countMeetings() {
        return meetingRepository.count();
    }

    @Override
    public long countByStatus(String status) {
        return meetingRepository.countByStatus(status);
    }

    @Override
    public boolean canAttend(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        MeetingConfig config = getMeetingConfig(meeting);
        return meeting != null && config != null && meeting.canAttend(config);
    }

    @Override
    public boolean canEditMeeting(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        MeetingConfig config = getMeetingConfig(meeting);
        return meeting != null && config != null && meeting.canEditMeeting(config);
    }

    @Override
    public boolean canImportShareholder(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        MeetingConfig config = getMeetingConfig(meeting);
        return meeting != null && config != null && meeting.canImportShareholder(config);
    }

    @Override
    public boolean canRegisterProxy(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        MeetingConfig config = getMeetingConfig(meeting);
        return meeting != null && config != null && meeting.canRegisterProxy(config);
    }

    @Override
    public boolean canVote(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
        MeetingConfig config = getMeetingConfig(meeting);
        return meeting != null && config != null && meeting.canVote(config);
    }

    private MeetingConfig getMeetingConfig(Meeting meeting) {
        if (meeting == null || meeting.getConfigId() == null) {
            return null;
        }
        return configRepository.findById(meeting.getConfigId()).orElse(null);
    }

    @Override
    public String getMeetingName(String meetingId) {
        if (meetingId == null || meetingId.isBlank()) {
            return null;
        }
        return meetingRepository.findById(meetingId)
                .map(Meeting::getTitle)
                .orElse(null);
    }

    @Override
    public String getFallbackMeetingId() {
        return meetingRepository.findOngoing()
                .map(Meeting::getId)
                .orElseGet(() -> meetingRepository.findAll().stream()
                        .map(Meeting::getId)
                        .findFirst()
                        .orElse(null));
    }
}
