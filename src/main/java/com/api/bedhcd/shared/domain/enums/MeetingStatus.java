package com.api.bedhcd.shared.domain.enums;

/**
 * Các trạng thái mặc định của cuộc họp.
 * Đã được chuyển từ Enum sang Constants để hỗ trợ trạng thái tùy chỉnh từ Admin.
 */
public class MeetingStatus {
    public static final String SCHEDULED = "SCHEDULED";
    public static final String ONGOING = "ONGOING";
    public static final String VOTING = "VOTING";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private MeetingStatus() {}
}
