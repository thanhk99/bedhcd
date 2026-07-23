package com.api.bedhcd.modules.meeting.application.port;

import com.api.bedhcd.modules.meeting.domain.model.MeetingRules;

public interface MeetingPort {
    /**
     * Lấy trạng thái hiện tại của cuộc họp
     */
    String getStatus(String meetingId);

    /**
     * Lấy bộ quy tắc nghiệp vụ cho trạng thái hiện tại của cuộc họp
     */
    MeetingRules getRules(String meetingId);

    /**
     * Lấy ID cấu hình đang áp dụng cho cuộc họp
     */
    String getConfigId(String meetingId);

    long countMeetings();

    long countByStatus(String status);

    /**
     * Kiểm tra xem cuộc họp có cho phép điểm danh hay không (ủy thác cho Domain
     * Meeting)
     */
    boolean canAttend(String meetingId);

    boolean canEditMeeting(String meetingId);

    boolean canImportShareholder(String meetingId);

    boolean canRegisterProxy(String meetingId);

    boolean canVote(String meetingId);

    boolean canDeleteMeeting(String meetingId);

    boolean canAddResolutionOrElection(String meetingId);

    boolean canEditResolutionOrElection(String meetingId);

    // --- Shareholder rules ---
    boolean canViewResolutionOrElection(String meetingId);

    boolean shareholderCanViewMeeting(String meetingId);

    boolean shareholderCanAction(String meetingId);

    boolean shareholderCanEditAccount(String meetingId);

    String getMeetingName(String meetingId);
}
