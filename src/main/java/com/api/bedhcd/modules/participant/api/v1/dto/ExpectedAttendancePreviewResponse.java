package com.api.bedhcd.modules.participant.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kết quả tạm tính (preview) khi KSNB import file danh sách tham dự dự kiến.
 * Chưa lưu gì vào DB - chỉ trả về để người dùng xác nhận trước khi import.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedAttendancePreviewResponse {
    /** Tổng số bản ghi (unique CCCD) trong file */
    private int totalRows;
    /** Số người đã khớp */
    private long matchedCount;
    /** Số người không khớp */
    private long mismatchedCount;
    /** Danh sách chi tiết đối soát (frontend lọc status = LECH để cảnh báo) */
    private List<ReconciliationItemResponse> items;
}
