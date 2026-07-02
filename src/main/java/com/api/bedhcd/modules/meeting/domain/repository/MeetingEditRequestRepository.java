package com.api.bedhcd.modules.meeting.domain.repository;

import com.api.bedhcd.modules.meeting.domain.model.MeetingEditRequest;

import java.util.List;
import java.util.Optional;

/**
 * Domain Repository Interface cho MeetingEditRequest.
 * Không có Spring annotation — thuần interface nghiệp vụ.
 */
public interface MeetingEditRequestRepository {

    Optional<MeetingEditRequest> findById(String id);

    List<MeetingEditRequest> findByMeetingId(String meetingId);

    List<MeetingEditRequest> findAllPending();

    /**
     * Kiểm tra xem cuộc họp đã có yêu cầu PENDING chưa.
     */
    boolean existsPendingForMeeting(String meetingId);

    MeetingEditRequest save(MeetingEditRequest request);
}
