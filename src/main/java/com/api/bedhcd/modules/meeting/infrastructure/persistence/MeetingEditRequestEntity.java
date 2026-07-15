package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity ánh xạ bảng meeting_edit_requests.
 * Lưu trữ lịch sử tất cả yêu cầu chỉnh sửa cuộc họp (kể cả đã duyệt / từ chối).
 */
@Entity
@Table(name = "meeting_edit_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingEditRequestEntity {

    @Id
    private String id;

    @Column(name = "meeting_id", nullable = false)
    private String meetingId;

    /**
     * ID của admin đã gửi yêu cầu
     */
    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    /**
     * Loại thao tác: UPDATE | DELETE
     */
    @Column(name = "action_type", nullable = false)
    private String actionType;

    /**
     * Trạng thái yêu cầu: PENDING | APPROVED | REJECTED
     */
    @Column(nullable = false)
    private String status;

    /**
     * JSON payload chứa dữ liệu thay đổi (null khi DELETE)
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * Ghi chú lý do từ chối
     */
    @Column(columnDefinition = "TEXT")
    private String note;

    /**
     * ID của admin đã duyệt / từ chối
     */
    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
