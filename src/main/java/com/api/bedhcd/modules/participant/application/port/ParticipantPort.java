package com.api.bedhcd.modules.participant.application.port;

public interface ParticipantPort {
    /**
     * Lấy tổng quyền biểu quyết của một người dùng trong cuộc họp
     */
    long getVotingPower(String meetingId, String userId);
    long getAttendingShares(String meetingId, String userId);
    long getReceivedProxyShares(String meetingId, String userId);
    long getDelegatedShares(String meetingId, String userId);

    /**
     * Kiểm tra người dùng đã điểm danh thành công chưa và lấy thời gian
     */
    boolean isCheckedIn(String meetingId, String userId);
    java.time.LocalDateTime getCheckedInAt(String meetingId, String userId);
    long countTotalParticipants();
    long countTotalCheckedIn();
    long sumTotalShares();
    long sumCheckedInShares();
    long countByMeetingId(String meetingId);
    long countCheckedInByMeetingId(String meetingId);
    long sumTotalSharesByMeetingId(String meetingId);
    long sumCheckedInSharesByMeetingId(String meetingId);
    String getLastMeetingId(String userId);
    void createParticipant(String meetingId, String userId, Long sharesOwned);
}
