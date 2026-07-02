package com.api.bedhcd.modules.meeting.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response trả về khi xem hoặc tạo yêu cầu chỉnh sửa cuộc họp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingEditRequestResponse {

    private String id;
    private String meetingId;
    private String requestedBy;

    /**
     * Loại thao tác: UPDATE | DELETE | UPDATE_STATUS
     */
    private String actionType;

    /**
     * Trạng thái: PENDING | APPROVED | REJECTED
     */
    private String status;

    /**
     * Payload JSON (có thể null khi DELETE)
     */
    private String payload;

    /**
     * Ghi chú lý do từ chối
     */
    private String note;

    private String reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    /**
     * Cho phép client biết yêu cầu cần approval
     */
    private boolean requiresApproval;
}
