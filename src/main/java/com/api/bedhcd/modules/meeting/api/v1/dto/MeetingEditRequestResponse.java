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
    private String requestedByName;

    /**
     * Loại thao tác: UPDATE | DELETE
     */
    private String actionType;

    /**
     * Trạng thái: PENDING | APPROVED | REJECTED
     */
    private String status;

    /**
     * Mô tả tự nhiên của thay đổi (vd: "Cập nhật cuộc họp 'X': Đổi Tên từ 'A'
     * thành 'B'").
     */
    private String description;

    /**
     * Danh sách thay đổi dạng text có quy ước, mỗi dòng:
     * fieldCode|fieldLabel|oldValue|newValue (∅ = không có giá trị).
     */
    private String changes;

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
