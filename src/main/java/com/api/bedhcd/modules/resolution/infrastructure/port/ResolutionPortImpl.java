package com.api.bedhcd.modules.resolution.infrastructure.port;

import com.api.bedhcd.modules.resolution.application.port.ResolutionPort;
import com.api.bedhcd.modules.resolution.domain.repository.ResolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResolutionPortImpl implements ResolutionPort {

    private final ResolutionRepository resolutionRepository;

    @Override
    public long countResolutions() {
        return resolutionRepository.count();
    }

    @Override
    public long countResolutionsByMeetingId(String meetingId) {
        return resolutionRepository.countByMeetingId(meetingId);
    }
}
