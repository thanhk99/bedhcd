package com.api.bedhcd.modules.resolution.application.port;

import java.util.List;

public interface ResolutionPort {
    long countResolutions();
    long countResolutionsByMeetingId(String meetingId);

    List<ResolutionSummary> getResolutionsByMeetingId(String meetingId);

    record ResolutionSummary(
        String resolutionId,
        String title,
        String description,
        Integer displayOrder,
        List<OptionSummary> options
    ) {}

    record OptionSummary(
        String optionId,
        String name,
        String type,
        Integer displayOrder
    ) {}
}
