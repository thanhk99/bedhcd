package com.api.bedhcd.modules.resolution.application.port;

public interface ResolutionPort {
    long countResolutions();
    long countResolutionsByMeetingId(String meetingId);
}
