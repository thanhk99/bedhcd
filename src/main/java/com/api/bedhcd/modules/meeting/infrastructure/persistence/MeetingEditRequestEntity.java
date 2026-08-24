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
     * Mô tả tự nhiên, đọc được nội dung thay đổi (vd: "Cập nhật cuộc họp 'X':
     * Đổi Tên từ 'A' thành 'B'").
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Danh sách thay đổi dạng text có quy ước (KHÔNG phải JSON), mỗi dòng:
     * fieldCode|fieldLabel|oldValue|newValue (∅ = không có giá trị).
     */
    @Column(columnDefinition = "TEXT")
    private String changes;

    /**
     * Ghi chú lý do từ chối
     */
    @Column(columnDefinition = "TEXT")
    private String note;
    
    /**
     * Dữ liệu JSON cho các batch request
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

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
