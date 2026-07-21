package com.api.bedhcd.modules.resolution.infrastructure.port;

import com.api.bedhcd.modules.resolution.application.port.ResolutionPort;
import com.api.bedhcd.modules.resolution.domain.repository.ResolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<ResolutionSummary> getResolutionsByMeetingId(String meetingId) {
        return resolutionRepository.findByMeetingId(meetingId).stream()
                .map(r -> new ResolutionSummary(
                        r.getId(),
                        r.getTitle(),
                        r.getDescription(),
                        r.getDisplayOrder(),
                        r.getOptions() == null ? List.of()
                                : r.getOptions().stream()
                                        .map(opt -> new OptionSummary(
                                                opt.getId(),
                                                opt.getName(),
                                                opt.getType() != null ? opt.getType().name() : null,
                                                opt.getDisplayOrder()))
                                        .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
