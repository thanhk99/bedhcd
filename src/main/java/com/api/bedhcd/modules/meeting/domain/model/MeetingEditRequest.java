package com.api.bedhcd.modules.meeting.domain.model;

import com.api.bedhcd.modules.meeting.domain.exception.MeetingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.api.bedhcd.shared.domain.UuidFactory;

/**
 * Aggregate Root: Yêu cầu chỉnh sửa cuộc họp cần được duyệt.
 * Bất kỳ thao tác nào (UPDATE nội dung, DELETE, đổi STATUS) do admin thường
 * thực hiện
 * đều phải qua bước duyệt từ SUPERADMIN hoặc admin có quyền APPROVE trên
 * MANAGE_MEETING.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingEditRequest {

    private String id;
    private String meetingId;

    /**
     * ID của admin đã gửi yêu cầu
     */
    private String requestedBy;

    /**
     * Loại thao tác: "UPDATE", "DELETE", "UPDATE_STATUS"
     */
    private String actionType;

    /**
     * Trạng thái hiện tại của yêu cầu
     */
    private EditRequestStatus status;

    /**
     * JSON chứa payload thay đổi (dùng cho UPDATE và UPDATE_STATUS).
     * Null khi actionType là DELETE.
     */
    private String payload;

    /**
     * Ghi chú lý do từ chối (do reviewer điền khi REJECT)
     */
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    /**
     * ID của admin đã duyệt / từ chối
     */
    private String reviewedBy;

    // ─── Factory Methods ───────────────────────────────────────────────────────

    /**
     * Tạo yêu cầu chỉnh sửa nội dung cuộc họp (UPDATE).
     */
    public static MeetingEditRequest createUpdateRequest(String meetingId, String requestedBy, String payloadJson) {
        return MeetingEditRequest.builder()
                .id(UuidFactory.generate())
                .meetingId(meetingId)
                .requestedBy(requestedBy)
                .actionType("UPDATE")
                .status(EditRequestStatus.PENDING)
                .payload(payloadJson)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Tạo yêu cầu xóa cuộc họp (DELETE).
     */
    public static MeetingEditRequest createDeleteRequest(String meetingId, String requestedBy) {
        return MeetingEditRequest.builder()
                .id(UuidFactory.generate())
                .meetingId(meetingId)
                .requestedBy(requestedBy)
                .actionType("DELETE")
                .status(EditRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Tạo yêu cầu đổi trạng thái cuộc họp (UPDATE_STATUS).
     */
    public static MeetingEditRequest createUpdateStatusRequest(String meetingId, String requestedBy, String newStatus) {
        return MeetingEditRequest.builder()
                .id(UuidFactory.generate())
                .meetingId(meetingId)
                .requestedBy(requestedBy)
                .actionType("UPDATE_STATUS")
                .status(EditRequestStatus.PENDING)
                .payload(newStatus)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── Business Methods ──────────────────────────────────────────────────────

    /**
     * Phê duyệt yêu cầu. Chỉ cho phép khi đang ở trạng thái PENDING.
     */
    public void approve(String reviewerId) {
        if (this.status != EditRequestStatus.PENDING) {
            throw MeetingException.editRequestNotPending(this.id);
        }
        this.status = EditRequestStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * Từ chối yêu cầu. Chỉ cho phép khi đang ở trạng thái PENDING.
     */
    public void reject(String reviewerId, String note) {
        if (this.status != EditRequestStatus.PENDING) {
            throw MeetingException.editRequestNotPending(this.id);
        }
        this.status = EditRequestStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.note = note;
    }

    public boolean isPending() {
        return EditRequestStatus.PENDING.equals(this.status);
    }
}
